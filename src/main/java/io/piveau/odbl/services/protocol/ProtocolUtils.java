package io.piveau.odbl.services.protocol;

import io.piveau.odbl.Constants;
import io.piveau.odbl.Utils;
import io.piveau.odbl.models.*;
import io.piveau.odbl.utils.CryptoManager;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class ProtocolUtils {

    Logger LOGGER = LoggerFactory.getLogger(ProtocolUtils.class);

    static public final String ADDR_BROADCAST = "protocol.broadcast";
    static public final String ADDR_ACTION = "protocol.action";
    static public final String ADDR_SYNC = "protocol.sync";
    static public final String ADDR_BLOCK = "protocol.block";
    static public final String ADDR_DEBUG = "protocol.debug";

    private WebClient webClient;
    private Vertx vertx;

    public ProtocolUtils(WebClient webClient, Vertx vertx) {
        this.webClient = webClient;
        this.vertx = vertx;
    }

    public void sendToNode(String url, JsonObject payload) {
        String fullUrl = url + "/protocol/action";
        webClient.postAbs(fullUrl)
            .putHeader("Content-Type", "application/json")
            .timeout(5000)
            .expect(ResponsePredicate.SC_ACCEPTED)
            .sendJsonObject(payload, httpResponseAsyncResult -> {
                if (httpResponseAsyncResult.succeeded()) {
                    //LOGGER.info("Successfully send message to: " + url);
                } else {
                    LOGGER.debug("Node " + url + " not reachable");
                }
            });
    }

    public void getNodeStatus(String url, Handler<AsyncResult<JsonObject>> result) {
        String fullUrl = url + "/status";
        webClient.getAbs(fullUrl)
            .putHeader("Content-Type", "application/json")
            .timeout(5000)
            .expect(ResponsePredicate.SC_OK)
            .send(ar -> {
                if(ar.succeeded()) {
                    result.handle(Future.succeededFuture(ar.result().bodyAsJsonObject()));
                } else {
                    LOGGER.error(ar.cause().getMessage());
                    result.handle(Future.failedFuture(ar.cause()));
                }
            });
    }


    public Block createNewBlock(Transaction transaction, Block prevBlock, String issuer) {
        Block block = new Block(
            prevBlock.index + 1,
            Utils.getCurrentTimestamp(),
            prevBlock.hash,
            null,
            transaction.id,
            CryptoManager.hashJson(transaction.toJson()),
            issuer,
            null
        );
        block.hash = CryptoManager.getHashForBlock(block);;
        block.signature = CryptoManager.fromContext(vertx).sign(block.hash);
        return block;
    }

    public Integer getMinApproval(JsonObject config) {
        JsonArray nodes = NetworkEnvironment.getPeers(vertx).getJsonArray("validPeers");
        int noNodes = nodes.size();
        return 2 * (noNodes / 3) + 1;
    }

    public boolean verifyProtocolMessage(ProtocolMessage protocolMessage) {
        String hash = CryptoManager.hashJson(protocolMessage.payload);
        Verification verification = Verification.fromJson(protocolMessage.verification);
        String publicKey = NetworkEnvironment.getPublicKeyForNode(vertx, verification.issuer);
        return CryptoManager.verify(hash, publicKey, verification.signature);
    }

    public JsonObject getRandomPeer() {
        JsonObject peerMeta = NetworkEnvironment.getPeers(vertx);
        JsonArray validPeers = peerMeta.getJsonArray("validPeers");
        int generatedLong = (int) (Math.random() * (validPeers.size()));
        return validPeers.getJsonObject(generatedLong);
    }

    public String getWebSocketURL(String url) {
        String baseUrl = url.substring(4);
        return "ws" + baseUrl;
    }

    public void findRandomAvailablePeer(Handler<AsyncResult<JsonObject>> result) {
        JsonObject peerMeta = NetworkEnvironment.getPeers(vertx);
        JsonArray validPeers = peerMeta.getJsonArray("validPeers");
        ArrayList<Integer> peerIndex = new ArrayList<>(validPeers.size());
        for(int i = 0; i < validPeers.size(); i++){
            peerIndex.add(i);
        }
        Collections.shuffle(peerIndex);

        getStatusForIndex(0, peerIndex, validPeers, ar -> {
            if(ar.succeeded()) {
                result.handle(Future.succeededFuture(ar.result()));
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });

    }

    private void getStatusForIndex(int index, ArrayList<Integer> order, JsonArray peers, Handler<AsyncResult<JsonObject>> result) {
        String url = peers.getJsonObject(order.get(index)).getString("url");
        getNodeStatus(url, ar -> {
            if(ar.succeeded()) {
                result.handle(Future.succeededFuture(peers.getJsonObject(order.get(index))));
            } else {
                if(index < order.size()-1) {
                    getStatusForIndex(index + 1, order, peers, result);
                } else {
                    result.handle(Future.failedFuture(ar.cause()));
                }
            }
        });
    }


}
