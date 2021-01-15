package io.piveau.odbl.handler;

import io.piveau.odbl.services.node.NodeService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class NodeHandler extends BaseHandler {

    Logger LOGGER = LoggerFactory.getLogger(NodeHandler.class);
    private final NodeService nodeService;

    public NodeHandler(Vertx vertx) {
        nodeService = NodeService.createProxy(vertx, NodeService.SERVICE_ADDRESS);
    }

    public void handlePostTransaction(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        if(getBooleanQueryParam(ctx, "debug", false)) {
            nodeService.postTransactionDebug(payload, ar -> {
                successWithMessage(ctx, ar.result().getValue("message").toString());
            });
        } else {
            nodeService.postTransaction(payload, ar -> {
                successWithMessage(ctx, ar.result().getValue("message").toString());
            });
        }
    }

    public void handleGetTransactions(RoutingContext ctx) {
        nodeService.getTransactions(
            getIntegerQueryParam(ctx, "page", 0),
            getIntegerQueryParam(ctx, "limit", 10),
            ar -> {
            successWithJson(ctx, ar.result());
        });
    }

    public void handleGetBlockchain(RoutingContext ctx) {
        nodeService.getBlockchain(
            getIntegerQueryParam(ctx, "page", 0),
            getIntegerQueryParam(ctx, "limit", 10),
            ar -> {
                successWithJson(ctx, ar.result());
            });
    }

    public void handleGetDatasets(RoutingContext ctx) {
        nodeService.getDatasets(
            getIntegerQueryParam(ctx, "page", 0),
            getIntegerQueryParam(ctx, "limit", 10),
            ar -> {
                if(ar.succeeded()) {
                    successWithJson(ctx, ar.result());
                } else {
                    errorWithMessage(ctx, ar.cause().getMessage());
                }
            });
    }

    public void handleGetDataset(RoutingContext ctx) {
        String id = ctx.pathParam("id");
        nodeService.getDataset(id, ar -> {
            if(ar.succeeded()) {
                successWithJson(ctx, ar.result());
            } else {
                notFound(ctx);
            }
        });
    }
}
