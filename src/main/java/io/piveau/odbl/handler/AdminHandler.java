package io.piveau.odbl.handler;

import io.piveau.odbl.services.admin.AdminService;
import io.piveau.odbl.services.node.NodeService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminHandler extends BaseHandler {

    Logger LOGGER = LoggerFactory.getLogger(AdminHandler.class);
    private final AdminService adminService;

    public AdminHandler(Vertx vertx) {
        adminService = AdminService.createProxy(vertx, AdminService.SERVICE_ADDRESS);
    }

    public void handlePostDatabase(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        adminService.postDatabase(payload, ar -> {
            accepted(ctx);
        });
    }

    public void handlePostNode(RoutingContext ctx) {
        JsonObject payload = ctx.getBodyAsJson();
        adminService.postNode(payload, ar -> {
            successWithJson(ctx, ar.result());
        });
    }


}
