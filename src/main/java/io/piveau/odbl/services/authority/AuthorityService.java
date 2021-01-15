package io.piveau.odbl.services.authority;

import io.piveau.odbl.persistence.MongoManager;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface AuthorityService {

    String SERVICE_ADDRESS = "io.piveau.odbl.authority";

    static AuthorityService create(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<AuthorityService>> readyHandler) {
        return new AuthorityServiceImpl(vertx, config, mongoManager, readyHandler);
    }

    static AuthorityService createProxy(Vertx vertx, String address) {
        return new AuthorityServiceVertxEBProxy(vertx, address);
    }

    void getPeers(Handler<AsyncResult<JsonObject>> result);

    void postDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> result);

    void postNode(JsonObject payload, Handler<AsyncResult<JsonObject>> result);

    void postNewPeer(String payload, Handler<AsyncResult<JsonObject>> result);

}
