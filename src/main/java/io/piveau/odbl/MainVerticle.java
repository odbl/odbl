package io.piveau.odbl;

import io.piveau.odbl.handler.*;
import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.persistence.PersistenceFactory;
import io.piveau.odbl.services.admin.AdminServiceVerticle;
import io.piveau.odbl.services.authority.AuthorityServiceVerticle;
import io.piveau.odbl.services.node.NodeServiceVerticle;
import io.piveau.odbl.services.protocol.ProtocolServiceVerticle;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

    Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        loadConfig().onSuccess(ar -> {
            LOGGER.info(ar.encodePrettily());
            if(ar.getString(Constants.MODE).equals("publisher")) {
                LOGGER.info("Starting in Publisher Mode");
                bootstrapPublisherVerticles(ar).compose(this::startPublisherNode).onComplete(startPromise);
            } else if(ar.getString(Constants.MODE).equals("authority")) {
                LOGGER.info("Starting in Authority Mode");
                bootstrapAuthorityVerticles(ar).compose(this::startAuthorityNode).onComplete(startPromise);
            } else {
                startPromise.fail("Application mode is not valid");
            }
        });
    }

    private Future<Void> startPublisherNode(JsonObject config) {
        Promise<Void> promise = Promise.promise();
        Integer port = config.getInteger(Constants.WEB_PORT);

        MainHandler mainHandler = new MainHandler(vertx, config);
        NodeHandler nodeHandler = new NodeHandler(vertx);
        ProtocolHandler protocolHandler = new ProtocolHandler(vertx);
        AdminHandler adminHandler = new AdminHandler(vertx);
        ScheduleHandler scheduleHandler = new ScheduleHandler(vertx);

        //vertx.setPeriodic(10000, scheduleHandler::healthHandler);

        OpenAPI3RouterFactory.create(vertx, "webroot/openapi.yaml", ar -> {
            if(ar.succeeded()) {
                OpenAPI3RouterFactory routerFactory = ar.result();

                // Create a artificial network delay
                routerFactory.addGlobalHandler(mainHandler::handleNetworkDelay);

                routerFactory.addHandlerByOperationId("status", mainHandler::handleGetStatus);
                routerFactory.addHandlerByOperationId("postTransaction", nodeHandler::handlePostTransaction);
                routerFactory.addHandlerByOperationId("getTransactions", nodeHandler::handleGetTransactions);
                routerFactory.addHandlerByOperationId("getBlockchain", nodeHandler::handleGetBlockchain);
                routerFactory.addHandlerByOperationId("getDatasets", nodeHandler::handleGetDatasets);
                routerFactory.addHandlerByOperationId("getDataset", nodeHandler::handleGetDataset);
                routerFactory.addHandlerByOperationId("postProtocolAction", protocolHandler::handleAction);
                routerFactory.addHandlerByOperationId("postDatabase", adminHandler::handlePostDatabase);
                routerFactory.addHandlerByOperationId("postAdminNode", adminHandler::handlePostNode);

                Router router = routerFactory.getRouter();
                router.errorHandler(400, routingContext -> {
                    if (routingContext.failure() instanceof ValidationException) {
                        String validationErrorMessage = routingContext.failure().getMessage();
                        routingContext.response().setStatusCode(400).end(new JsonObject().put("error", validationErrorMessage).encode());
                    } else {
                        // Unknown 400 failure happened
                        routingContext.response().setStatusCode(400).end();
                    }
                });
                router.route("/*").handler(StaticHandler.create());

                HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                    .setPort(port)
                    .setMaxWebSocketFrameSize(1000000000)
                    .setMaxWebSocketMessageSize(10000000));
                server.webSocketHandler(protocolHandler::syncHandler);
                server.requestHandler(router).listen(serverResult -> {
                    if(serverResult.succeeded()) {
                        LOGGER.info("Successfully launched server on port [{}]", port);
                    } else {
                        LOGGER.error("Failed to start server at [{}]: {}", port, serverResult.cause());
                    }
                });

                promise.complete();
            } else {
                LOGGER.error("Failed to start server at [{}]: {}", port, ar.cause());
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    private Future<JsonObject> bootstrapPublisherVerticles(JsonObject config) {
        Promise<JsonObject> promise = Promise.promise();

        MongoManager mongoManager = PersistenceFactory.getMongoDBManager(vertx,
            config.getString(Constants.MONGO_HOST),
            config.getInteger(Constants.MONGO_PORT),
            config.getString(Constants.MONGO_DB));

        mongoManager.initDB(ar -> {
            if(ar.succeeded()) {
                NetworkEnvironment networkEnvironment = new NetworkEnvironment(mongoManager, vertx, config);
                networkEnvironment.initCache(ar2 -> {
                    if(ar2.succeeded()) {
                        DeploymentOptions nodeOptions = new DeploymentOptions();
                        Promise<String> nodePromise = Promise.promise();
                        NodeServiceVerticle nodeServiceVerticle = new NodeServiceVerticle(mongoManager, config);
                        vertx.deployVerticle(nodeServiceVerticle, nodeOptions, nodePromise);

                        DeploymentOptions protocolOptions = new DeploymentOptions();
                        protocolOptions.setWorker(true);
                        Promise<String> protocolPromise = Promise.promise();
                        ProtocolServiceVerticle protocolServiceVerticle = new ProtocolServiceVerticle(mongoManager, config, vertx);
                        vertx.deployVerticle(protocolServiceVerticle, protocolOptions, protocolPromise);

                        DeploymentOptions adminOptions =  new DeploymentOptions();
                        Promise<String> adminPromise = Promise.promise();
                        AdminServiceVerticle adminServiceVerticle = new AdminServiceVerticle(mongoManager, config);
                        vertx.deployVerticle(adminServiceVerticle, adminOptions, adminPromise);

                        CompositeFuture.all(Arrays.asList(
                            nodePromise.future(),
                            protocolPromise.future(),
                            adminPromise.future()
                        )).onComplete(ar3 -> {
                            if(ar3.succeeded()) {
                                promise.complete(config);
                            } else {
                                promise.fail(ar.cause());
                            }
                        });

                    } else {
                        LOGGER.error(ar2.cause().getMessage());
                        promise.fail(ar2.cause());
                    }
                });
            } else {
                LOGGER.error(ar.cause().getMessage());
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    private Future<Void> startAuthorityNode(JsonObject config) {
        Promise<Void> promise = Promise.promise();
        Integer port = config.getInteger(Constants.WEB_PORT);

        MainHandler mainHandler = new MainHandler(vertx, config);
        AuthorityHandler authorityHandler = new AuthorityHandler(vertx, config);

        OpenAPI3RouterFactory.create(vertx, "webroot/authority.openapi.yaml", ar -> {
            if(ar.succeeded()) {
                OpenAPI3RouterFactory routerFactory = ar.result();

                // Create a artificial network delay
                routerFactory.addGlobalHandler(mainHandler::handleNetworkDelay);

                routerFactory.addHandlerByOperationId("status", authorityHandler::handleGetStatus);
                routerFactory.addHandlerByOperationId("getPeers", authorityHandler::handleGetPeers);
                routerFactory.addHandlerByOperationId("postDatabase", authorityHandler::handlePostDatabase);
                routerFactory.addHandlerByOperationId("postAdminNode", authorityHandler::handlePostNode);
                routerFactory.addHandlerByOperationId("postAdminNewPeer", authorityHandler::handlePostNewPeer);

                Router router = routerFactory.getRouter();
                router.errorHandler(400, routingContext -> {
                    if (routingContext.failure() instanceof ValidationException) {
                        String validationErrorMessage = routingContext.failure().getMessage();
                        routingContext.response().setStatusCode(400).end(new JsonObject().put("error", validationErrorMessage).encode());
                    } else {
                        // Unknown 400 failure happened
                        routingContext.response().setStatusCode(400).end();
                    }
                });
                router.route("/*").handler(StaticHandler.create().setIndexPage("authority.index.html"));

                HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(port));
                server.requestHandler(router).listen(serverResult -> {
                    if(serverResult.succeeded()) {
                        LOGGER.info("Successfully launched server on port [{}]", port);
                    } else {
                        LOGGER.error("Failed to start server at [{}]: {}", port, serverResult.cause());
                    }
                });

                promise.complete();
            } else {
                LOGGER.error("Failed to start server at [{}]: {}", port, ar.cause());
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }


    private Future<JsonObject> bootstrapAuthorityVerticles(JsonObject config) {
        Promise<JsonObject> promise = Promise.promise();

        MongoManager mongoManager = PersistenceFactory.getMongoDBManager(vertx,
            config.getString(Constants.MONGO_HOST),
            config.getInteger(Constants.MONGO_PORT),
            config.getString(Constants.MONGO_DB));

        mongoManager.initAuthorityDB(ar -> {
            if(ar.succeeded()) {
                DeploymentOptions authorityOptions = new DeploymentOptions();
                Promise<String> authorityPromise = Promise.promise();
                AuthorityServiceVerticle authorityServiceVerticle = new AuthorityServiceVerticle(mongoManager, config);
                vertx.deployVerticle(authorityServiceVerticle, authorityOptions, authorityPromise);

                CompositeFuture.all(Arrays.asList(
                    authorityPromise.future()
                )).onComplete(ar2 -> {
                    if(ar2.succeeded()) {
                        promise.complete(config);
                    } else {
                        promise.fail(ar2.cause());
                    }
                });

            } else {
                LOGGER.error(ar.cause().getMessage());
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }


    private Future<JsonObject> loadConfig() {
        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
            .setType("env")
            .setConfig(new JsonObject().put("keys", new JsonArray()
                .add(Constants.WEB_PORT)
                .add(Constants.MODE)
                .add(Constants.URL)
                .add(Constants.NAME)
                .add(Constants.MONGO_DB)
                .add(Constants.MONGO_HOST)
                .add(Constants.MONGO_PORT)
                .add(Constants.NODES)
                .add(Constants.NODE_ID)
                .add(Constants.NETWORK_DELAY)
                .add(Constants.AUTHORITY_NODE)
                .add(Constants.INITIAL_PRIMARY)
                .add(Constants.AUTO_SYNC)
                .add(Constants.TACT)
            ));

        String configPath = "conf/config.json";
        String configFileName = System.getenv("CONFIG_FILE_NAME");
        if(configFileName != null) {
            configPath = "conf/" + configFileName;
        }
        LOGGER.info(configFileName);
        ConfigStoreOptions fileStoreOptions = new ConfigStoreOptions()
            .setType("file")
            .setConfig(new JsonObject().put("path", configPath));

        ConfigRetriever retriever = ConfigRetriever
            .create(vertx, new ConfigRetrieverOptions()
                .addStore(fileStoreOptions)
                .addStore(envStoreOptions));
        Promise<JsonObject> promise = Promise.promise();
        retriever.getConfig(promise);
        return promise.future();
    }


}
