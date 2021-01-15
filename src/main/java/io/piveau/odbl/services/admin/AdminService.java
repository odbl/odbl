package io.piveau.odbl.services.admin;

import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.node.NodeServiceImpl;
import io.piveau.odbl.services.node.NodeServiceVertxEBProxy;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface AdminService {

    String SERVICE_ADDRESS = "io.piveau.odbl.admin";

    static AdminService create(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<AdminService>> readyHandler) {
        return new AdminServiceImpl(vertx, config, mongoManager, readyHandler);
    }

    static AdminService createProxy(Vertx vertx, String address) {
        return new AdminServiceVertxEBProxy(vertx, address);
    }

    void postDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> result);

    void postNode(JsonObject payload, Handler<AsyncResult<JsonObject>> result);

}
