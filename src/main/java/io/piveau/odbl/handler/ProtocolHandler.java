package io.piveau.odbl.handler;

import io.piveau.odbl.services.protocol.ProtocolUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolHandler extends BaseHandler {

    Logger LOGGER = LoggerFactory.getLogger(ProtocolHandler.class);

    private Vertx vertx;

    public ProtocolHandler(Vertx vertx) {
        this.vertx =  vertx;
    }

    public void syncHandler(ServerWebSocket webSocket) {
        if(webSocket.path().equals("/protocol/sync")) {
            webSocket.accept();
            LOGGER.debug("Established Sync-Socket from {}", webSocket.remoteAddress().host());
            webSocket.frameHandler(frame -> {
                if(frame.isText()) {
                    JsonObject payload = new JsonObject(frame.textData());
                    vertx.eventBus().request(ProtocolUtils.ADDR_BLOCK, payload, reply -> {
                        if(reply.succeeded()) {
                            JsonObject response = (JsonObject) reply.result().body();
                            //LOGGER.debug(response.encodePrettily());
                            webSocket.writeTextMessage(response.encode());
                        }
                    });
                }
            });
            webSocket.closeHandler(ar -> {
                LOGGER.debug("Closed Sync-Socket");
            });
        } else {
            webSocket.reject();
        }
    }

    public void handleAction(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        vertx.eventBus().send(ProtocolUtils.ADDR_ACTION, payload);
        accepted(ctx);
    }

}
