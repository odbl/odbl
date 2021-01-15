package io.piveau.odbl.handler;

import io.netty.util.Constant;
import io.piveau.odbl.Constants;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class MainHandler extends BaseHandler {

    Logger LOGGER = LoggerFactory.getLogger(MainHandler.class);
    JsonObject config;
    Vertx vertx;

    public MainHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }

    public void handleGetStatus(RoutingContext ctx) {
        JsonObject result = new JsonObject();
        JsonObject nodeInformation = NetworkEnvironment.getMeta(vertx);
        if(nodeInformation == null) {
            result.put("status", "not initialized");
        } else {
            result.put("status", "running");
            result.put("nodeID", nodeInformation.getValue("nodeID"));
            JsonObject nodeKeys = NetworkEnvironment.getKeys(vertx);
            result.put("publicKey", nodeKeys.getJsonArray("keys").getJsonObject(0).getValue("publicKey"));
            result.put("peers", NetworkEnvironment.getPeers(vertx));
        }
        ctx.response()
            .setStatusCode(200)
            .end(result.encode());
    }

    public void handleNetworkDelay(RoutingContext ctx) {
        JsonObject delayConfig = config.getJsonObject(Constants.NETWORK_DELAY);
        if(delayConfig.getBoolean("enabled")) {
            long leftLimit = delayConfig.getLong("min");
            long rightLimit = delayConfig.getLong("max");;
            long generatedLong = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
            LOGGER.info("Delaying for " + generatedLong + " ms");
            vertx.setTimer(generatedLong, timer -> {
                ctx.next();
            });
        } else {
            ctx.next();
        }
    }

}
