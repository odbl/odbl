package io.piveau.odbl.services.authority;

import io.piveau.odbl.Utils;
import io.piveau.odbl.models.Block;
import io.piveau.odbl.models.ProtocolMessage;
import io.piveau.odbl.models.Transaction;
import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.node.NodeService;
import io.piveau.odbl.services.protocol.ProtocolMessageFactory;
import io.piveau.odbl.services.protocol.ProtocolUtils;
import io.piveau.odbl.utils.CryptoManager;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AuthorityServiceImpl implements AuthorityService {

    Logger LOGGER = LoggerFactory.getLogger(AuthorityServiceImpl.class);

    private final MongoManager mng;
    private Vertx vertx;
    private JsonObject config;
    private ProtocolMessageFactory protocolMessageFactory;
    private WebClient webClient;
    private ProtocolUtils protocolUtils;

    public AuthorityServiceImpl(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<AuthorityService>> readyHandler) {
        this.mng = mongoManager;
        this.vertx = vertx;
        this.config = config;
        this.protocolMessageFactory = new ProtocolMessageFactory(vertx);
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setKeepAlive(false);
        this.webClient = WebClient.create(vertx, webClientOptions);
        this.protocolUtils = new ProtocolUtils(webClient, vertx);
        readyHandler.handle(Future.succeededFuture(this));
    }


    @Override
    public void getPeers(Handler<AsyncResult<JsonObject>> result) {
        mng.fetchPeers(ar -> {
            if(ar.succeeded()) {
                JsonObject response = new JsonObject();
                response.put("success", true);
                response.put("count", ar.result().size());
                JsonArray results = new JsonArray();
                for(JsonObject peer : ar.result()) {
                    peer.remove("_id");
                    results.add(peer);
                }
                response.put("results", results);
                result.handle(Future.succeededFuture(response));
            } else {
                LOGGER.debug(ar.cause().getMessage());
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void postDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {

    }

    @Override
    public void postNode(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {

    }

    @Override
    public void postNewPeer(String payload, Handler<AsyncResult<JsonObject>> result) {
        JsonObject response = new JsonObject();
        try {
            JsonObject identityCard = new JsonObject(new String(Base64.getDecoder().decode(payload)));
            LOGGER.debug(identityCard.encodePrettily());
            protocolUtils.getNodeStatus(identityCard.getString("url"), ar -> {
                if(ar.succeeded()) {
                    LOGGER.debug(ar.result().encodePrettily());
                    JsonObject nodeStatus = ar.result();
                    boolean verify = NetworkEnvironment.verifyIdentityCard(identityCard, nodeStatus.getString("publicKey"));
                    if(!verify) {
                        response.put("status", "error").put("message", "Verfication of Identity Card failed");
                        result.handle(Future.succeededFuture(response));
                    } else {
                        JsonObject peer = new JsonObject();
                        peer.put("id", identityCard.getString("nodeID"));
                        peer.put("name", identityCard.getString("name"));
                        peer.put("url", identityCard.getString("url"));
                        peer.put("publicKeys", new JsonArray().add(new JsonObject()
                            .put("timestamp", Utils.getCurrentTimestamp())
                            .put("publicKey", nodeStatus.getString("publicKey"))

                        ));
                        mng.upsertPeer(peer, ar2 -> {
                            if(ar2.succeeded()) {
                                response.put("status", "ok").put("message", "Added Peer");
                                ProtocolMessage message = protocolMessageFactory.createAuthorityMessage(
                                    new JsonObject()
                                    .put("action", "peersUpdated")
                                );
                                broadcast(message);
                                LOGGER.debug("Successfully added peer {}", peer.getString("name"));
                            } else {
                                LOGGER.error(ar2.cause().getMessage());
                                response.put("status", "error").put("message", ar2.cause().getMessage());
                            }
                            result.handle(Future.succeededFuture(response));
                        });
                    }
                } else {
                    response.put("status", "error").put("message", ar.cause().getMessage());
                    result.handle(Future.succeededFuture(response));
                }
            });

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            result.handle(Future.succeededFuture(response));
        }
    }

    private void broadcast(ProtocolMessage message) {
        LOGGER.debug("Broadcast {}", message.getIdentity());
        mng.fetchPeers(ar -> {
            if(ar.succeeded()) {
                List<JsonObject> peers = ar.result();
                peers.forEach(peer -> {
                    protocolUtils.sendToNode(peer.getString("url"), message.toJson());
                });
            } else {
                LOGGER.error(ar.cause().getMessage());
            }
        });
    }
}
