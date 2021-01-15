package io.piveau.odbl.utils;

import io.piveau.odbl.Constants;
import io.piveau.odbl.Utils;
import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.admin.AdminServiceImpl;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.PresentationDirection;
import java.lang.module.Configuration;
import java.util.Arrays;
import java.util.Base64;

public class NetworkEnvironment {

    Logger LOGGER = LoggerFactory.getLogger(NetworkEnvironment.class);

    static public final String META_NODE = "meta";
    static public final String META_KEYS = "keys";
    static public final String META_PEERS = "peers";
    static public final String META_PEER_KEYS = "peerKeys";

    private MongoManager mongoManager;
    private Vertx vertx;
    private JsonObject config;
    private WebClient webClient;

    public NetworkEnvironment(MongoManager mongoManager, Vertx vertx, JsonObject config) {
        this.mongoManager = mongoManager;
        this.vertx = vertx;
        this.config = config;
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setKeepAlive(false);
        this.webClient = WebClient.create(vertx, webClientOptions);
    }

    public void init() {
       initMeta(ar -> {});
       initKeys(ar -> {});
    }

    public void initCache(Handler<AsyncResult<Void>> result) {
        Promise<String> metaPromise = Promise.promise();
        mongoManager.fetchMetaObject(META_NODE, ar -> {
            if(ar.succeeded()) {
                storeInSharedData(META_NODE, ar.result());
                metaPromise.complete();
            } else {
                initMeta(ar2 -> {
                    if(ar2.succeeded()) {
                        storeInSharedData(META_NODE, ar2.result());
                        metaPromise.complete();
                    } else {
                        metaPromise.fail(ar2.cause());
                    }
                });
            }
        });
        Promise<String> keysPromise = Promise.promise();
        mongoManager.fetchMetaObject(META_KEYS, ar -> {
            if(ar.succeeded()) {
                storeInSharedData(META_KEYS, ar.result());
                keysPromise.complete();
            } else {
                initKeys(ar2 -> {
                    if(ar2.succeeded()) {
                        storeInSharedData(META_KEYS, ar2.result());
                        keysPromise.complete();
                    } else {
                        keysPromise.fail(ar2.cause());
                    }
                });
            }
        });
        Promise<String> peersPromise = Promise.promise();
        mongoManager.fetchMetaObject(META_PEERS, ar -> {
            if(ar.succeeded()) {
                JsonObject peers = ar.result();
                storeInSharedData(META_PEERS, peers);
                storePeerMap(peers);
                peersPromise.complete();
            }  else {
                initPeers(ar2 -> {
                    if(ar2.succeeded()) {
                        storeInSharedData(META_PEERS, ar2.result());
                        storePeerMap(ar2.result());
                        peersPromise.complete();
                    } else {
                        peersPromise.fail(ar2.cause());
                    }
                });
            }
        });
        CompositeFuture.all(Arrays.asList(
            metaPromise.future(),
            keysPromise.future(),
            peersPromise.future()
        )).onSuccess(ar -> {
            LOGGER.info("Init NetworkEnvironment");
            result.handle(Future.succeededFuture());
        }).onFailure(ar -> {
            result.handle(Future.failedFuture(ar.getCause()));
        });
    }

    private void storePeerMap(JsonObject peerMeta) {
        JsonObject peerKeys = new JsonObject();
        peerMeta.getJsonArray("validPeers").forEach(peer -> {
            if(peer instanceof JsonObject) {
                JsonObject peerObject = ((JsonObject) peer);
                peerKeys.put(
                    peerObject.getString("id"),
                    peerObject.getJsonArray("publicKeys").getJsonObject(0).getString("publicKey"));
            }
        });
        LOGGER.debug(peerKeys.encodePrettily());
        storeInSharedData(META_PEER_KEYS, peerKeys);
    }

    private void storeInSharedData(String id, JsonObject data) {
        data.remove("_id");
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("network");
        map.put(id, data);
    }

    public static JsonObject getMeta(Vertx vertx) {
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("network");
        JsonObject result = map.getOrDefault(META_NODE, null);
        return result;
    }

    public static JsonObject getPeers(Vertx vertx) {
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("network");
        return map.getOrDefault(META_PEERS, null);
    }

    public static JsonObject getKeys(Vertx vertx) {
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("network");
        JsonObject result = map.getOrDefault(META_KEYS, null);
        return result;
    }

    public static String getPublicKeyForNode(Vertx vertx, String nodeID) {
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("network");
        JsonObject result = map.getOrDefault(META_PEER_KEYS, null);
        if(result != null) {
            return result.getString(nodeID);
        } else {
            return null;
        }
    }

    public static String getNodeID(Vertx vertx) {
        return getMeta(vertx).getString("nodeID");
    }

    public static boolean isCurrentNodePrimary(Vertx vertx) {
        return getNodeID(vertx).equals(getPeers(vertx).getString("primary"));
    }


    public void initKeys(Handler<AsyncResult<JsonObject>> result) {
        CryptoManager cryptoManager = new CryptoManager();
        cryptoManager.generateKeyPair();

        JsonObject nodeKeys = new JsonObject().put("id", META_KEYS);
        JsonObject keyPair = new JsonObject();
        keyPair.put("timestamp", Utils.getCurrentTimestamp());
        keyPair.put("privateKey", cryptoManager.getPrivateKeyBase64());
        keyPair.put("publicKey", cryptoManager.getPublicKeyBase64());
        nodeKeys.put("keys", new JsonArray().add(keyPair));

        mongoManager.upsertMetaObject(nodeKeys, ar -> {
            if(ar.succeeded()) {
                result.handle(Future.succeededFuture(ar.result()));
                LOGGER.info("Created Keypair");
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void initPeers(Handler<AsyncResult<JsonObject>> result) {
        fetchPeers(ar -> {
            JsonObject nodePeers = new JsonObject().put("id", META_PEERS);
            nodePeers.put("primary", config.getString(Constants.INITIAL_PRIMARY));
            if(ar.succeeded()) {
                nodePeers.put("validPeers", ar.result());
            } else {
                nodePeers.put("validPeers", new JsonArray());
            }
            mongoManager.upsertMetaObject(nodePeers, ar2 -> {
                if(ar2.succeeded()) {
                    result.handle(Future.succeededFuture(ar2.result()));
                    LOGGER.info("Stored Peer Data");
                } else {
                    result.handle(Future.failedFuture(ar2.cause()));
                }
            });
        });
   }

    public void initMeta(Handler<AsyncResult<JsonObject>> result) {
        JsonObject nodeMetaInformation =  new JsonObject().put("id", META_NODE);
        nodeMetaInformation.put("nodeID", config.getValue(Constants.NODE_ID));
        nodeMetaInformation.put("name", config.getValue(Constants.NAME));
        nodeMetaInformation.put("url", config.getValue(Constants.URL));
        mongoManager.upsertMetaObject(nodeMetaInformation, ar -> {
            if(ar.succeeded()) {
                result.handle(Future.succeededFuture(ar.result()));
                LOGGER.info("Stored Node Meta Information");
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void fetchPeers(Handler<AsyncResult<JsonArray>> result) {
        String authorityUrl = config.getString(Constants.AUTHORITY_NODE);
        String fullUrl = authorityUrl + "/node/peers";
        webClient.getAbs(fullUrl)
            .putHeader("Content-Type", "application/json")
            .timeout(5000)
            .expect(ResponsePredicate.SC_OK)
            .send(ar -> {
                if(ar.succeeded()) {
                    JsonObject response = ar.result().bodyAsJsonObject();
                    result.handle(Future.succeededFuture(response.getJsonArray("results")));
                } else {
                    LOGGER.error(ar.cause().getMessage());
                    result.handle(Future.failedFuture(ar.cause()));
                }
            });
    }

    public void updatePeers(Handler<AsyncResult<Void>> result) {
        fetchPeers(ar -> {
            if(ar.succeeded()) {
                mongoManager.fetchMetaObject(META_PEERS, ar2 -> {
                    if(ar2.succeeded()) {
                        JsonObject currentPeerMeta = ar2.result();
                        JsonArray newPeerData = ar.result();
                        currentPeerMeta.put("validPeers", newPeerData);
                        mongoManager.upsertMetaObject(currentPeerMeta, ar3 -> {
                            if(ar3.succeeded()) {
                                storeInSharedData(META_PEERS, currentPeerMeta);
                                JsonObject peerKeys = new JsonObject();
                                newPeerData.forEach(peer -> {
                                    if(peer instanceof JsonObject) {
                                        JsonObject peerObject = ((JsonObject) peer);
                                        peerKeys.put(
                                            peerObject.getString("id"),
                                            peerObject.getJsonArray("publicKeys").getJsonObject(0).getString("publicKey"));
                                    }
                                });
                                storeInSharedData(META_PEER_KEYS, peerKeys);
                                LOGGER.info("Stored Peer Data");
                                result.handle(Future.succeededFuture());
                            } else {
                                LOGGER.error(ar3.cause().getMessage());
                                result.handle(Future.failedFuture(ar3.cause()));
                            }
                        });
                    } else {
                        LOGGER.error(ar2.cause().getMessage());
                        result.handle(Future.failedFuture(ar2.cause()));
                    }
                });
            } else {
                LOGGER.error(ar.cause().getMessage());
                result.handle(Future.failedFuture(ar.cause()));
            }
        });

    }


    public void setNewPrimary(String newPrimary, Handler<AsyncResult<String>> result) {
        JsonObject peersMeta = getPeers(vertx);
        JsonObject nextPrimary = peersMeta.getJsonArray("validPeers").getJsonObject(Integer.parseInt(newPrimary));
        peersMeta.put("primary", nextPrimary.getString("id"));
        //peersMeta.put("primary", "node01");
        mongoManager.upsertMetaObject(peersMeta, ar -> {
            if(ar.succeeded()) {
                storeInSharedData(META_PEERS, peersMeta);
                result.handle(Future.succeededFuture(nextPrimary.getString("id")));
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public String getNextPrimary() {
        String currentPrimary = getPeers(vertx).getString("primary");
        JsonArray peers = getPeers(vertx).getJsonArray("validPeers");
        String nextPrimary = "0";
        for(int i = 0; i < peers.size(); i++) {
            JsonObject peer = peers.getJsonObject(i);
            if (peer.getString("id").equals(currentPrimary)) {
                int nextID = i + 1;
                if(nextID < peers.size()) {
                    nextPrimary = String.valueOf(nextID);
                }
                break;
            }
        }
        return nextPrimary;
    }

    public String getIdentityCard() {
        JsonObject card = new JsonObject();
        JsonObject meta = getMeta(vertx);
        card.put("nodeID", meta.getString("nodeID"));
        card.put("name", meta.getString("name"));
        card.put("url", meta.getString("url"));
        CryptoManager cryptoManager = CryptoManager.fromContext(vertx);
        card.put("signature", cryptoManager.sign(CryptoManager.hashJson(card)));
        return Base64.getEncoder().encodeToString(card.encode().getBytes());
    }

    public static boolean verifyIdentityCard(JsonObject identityCard, String publicKey) {
        JsonObject hashCard = new JsonObject();
        hashCard.put("nodeID", identityCard.getString("nodeID"));
        hashCard.put("name", identityCard.getString("name"));
        hashCard.put("url", identityCard.getString("url"));
        String message = CryptoManager.hashJson(hashCard);
        return CryptoManager.verify(message, publicKey, identityCard.getString("signature"));
    }

}
