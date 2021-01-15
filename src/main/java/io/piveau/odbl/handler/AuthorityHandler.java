package io.piveau.odbl.handler;

import io.piveau.odbl.services.authority.AuthorityService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorityHandler extends BaseHandler {

    Logger LOGGER = LoggerFactory.getLogger(AuthorityHandler.class);

    JsonObject config;
    Vertx vertx;
    private final AuthorityService authorityService;

    public AuthorityHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
        authorityService = AuthorityService.createProxy(vertx, AuthorityService.SERVICE_ADDRESS);
    }

    public void handleGetStatus(RoutingContext ctx) {
        JsonObject result = new JsonObject();
        result.put("status", "running");
        ctx.response()
            .setStatusCode(200)
            .end(result.encode());
    }

    public void handlePostDatabase(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        authorityService.postDatabase(payload, ar -> {
            accepted(ctx);
        });
    }

    public void handlePostNode(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        authorityService.postNode(payload, ar -> {
            successWithJson(ctx, ar.result());
        });
    }

    public void handlePostNewPeer(RoutingContext ctx) {
        String payload = ctx.getBodyAsString();
        authorityService.postNewPeer(payload, ar -> {
            successWithJson(ctx, ar.result());
        });
    }

    public void handleGetPeers(RoutingContext ctx) {
        authorityService.getPeers(ar -> {
            successWithJson(ctx, ar.result());
        });
    }




}
