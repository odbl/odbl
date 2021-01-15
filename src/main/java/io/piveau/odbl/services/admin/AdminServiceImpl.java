package io.piveau.odbl.services.admin;

import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.protocol.ProtocolUtils;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AdminServiceImpl implements AdminService {

    Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final MongoManager mng;
    private Vertx vertx;
    private JsonObject config;

    public AdminServiceImpl(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<AdminService>> readyHandler) {
        this.mng = mongoManager;
        this.vertx = vertx;
        this.config = config;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public void postDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {
        String action = payload.getString("action");
        switch (action) {
            case "reset":
                result.handle(Future.succeededFuture(new JsonObject().put("message", "Database action accepted")));
                mng.dropCollections(ar -> {
                    if(ar.succeeded()) {
                        LOGGER.info("Dropped all collections");
                        mng.initDB(ar2 -> {
                            LOGGER.info("Created all collections");
                        });
                    }
                });
                break;
            default:
                result.handle(Future.failedFuture("Unkwown action"));
                break;
        }
    }

    @Override
    public void postNode(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {
        String action = payload.getString("action");
        NetworkEnvironment networkEnvironment = new NetworkEnvironment(mng, vertx, config);
        JsonObject response = new JsonObject();
        switch (action) {
            case "init":
                networkEnvironment.init();
                result.handle(Future.succeededFuture(new JsonObject().put("message", "Init Action accepted")));
                break;
            case "initPeers":
                networkEnvironment.initPeers(ar -> {});
                result.handle(Future.succeededFuture(new JsonObject().put("message", "Init Peers Action accepted")));
                break;
            case "status":
                response.put("meta", NetworkEnvironment.getMeta(vertx));
                response.put("keys", NetworkEnvironment.getKeys(vertx));
                response.put("peers", NetworkEnvironment.getPeers(vertx));
                result.handle(Future.succeededFuture(response));
                break;
            case "identityCard":
                response.put("nodeID", NetworkEnvironment.getMeta(vertx).getString("nodeID"));
                response.put("identityCard", networkEnvironment.getIdentityCard());
                result.handle(Future.succeededFuture(response));
                break;
            case "sync":
                vertx.eventBus().send(ProtocolUtils.ADDR_SYNC, payload);
                result.handle(Future.succeededFuture(new JsonObject().put("message", "Triggered Synchronization")));
                break;
            default:
                result.handle(Future.failedFuture("Unkwown action"));
                break;
        }
    }
}
