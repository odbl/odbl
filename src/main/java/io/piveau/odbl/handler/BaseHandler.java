package io.piveau.odbl.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public abstract class BaseHandler {

    protected void successWithMessage(RoutingContext ctx, String message, int statusCode) {
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("message", message).encode());
    }

    protected void successWithJson(RoutingContext ctx, JsonObject json, int statusCode) {
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end(json.encodePrettily());
    }

    protected void errorWithMessage(RoutingContext ctx, String message) {
        ctx.response()
            .setStatusCode(500)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("message", message).encode());
    }

    protected void notFound(RoutingContext ctx) {
        ctx.response()
            .setStatusCode(404)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("message", "Not Found").encode());
    }

    protected void successWithJson(RoutingContext ctx, JsonObject json) {
        successWithJson(ctx, json, 200);
    }

    protected void successWithMessage(RoutingContext ctx, String message) {
        successWithMessage(ctx, message, 200);
    }

    protected void accepted(RoutingContext ctx) {
        ctx.response().setStatusCode(202).end();
    }

    protected Integer getIntegerQueryParam(RoutingContext ctx, String param, Integer fallback) {
        if(ctx.queryParam(param).size() > 0) {
            String value = ctx.queryParam(param).get(0);
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        } else {
            return fallback;
        }
    }

    protected boolean getBooleanQueryParam(RoutingContext ctx, String param, Boolean fallback) {
        if(ctx.queryParam(param).size() > 0) {
            String value = ctx.queryParam(param).get(0);
            try {
                return Boolean.parseBoolean(value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        } else {
            return fallback;
        }
    }
}
