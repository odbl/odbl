package io.piveau.odbl.services.node;

import io.piveau.odbl.persistence.MongoManager;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface NodeService {

    String SERVICE_ADDRESS = "io.piveau.odbl.node";

    static NodeService create(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<NodeService>> readyHandler) {
        return new NodeServiceImpl(vertx, config, mongoManager, readyHandler);
    }

    static NodeService createProxy(Vertx vertx, String address) {
        return new NodeServiceVertxEBProxy(vertx, address);
    }

    void postTransactionDebug(JsonObject transaction, Handler<AsyncResult<JsonObject>> result);

    void postTransaction(JsonObject transaction, Handler<AsyncResult<JsonObject>> result);

    void getTransactions(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result);

    void getBlockchain(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result);

    void getDatasets(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result);

    void getDataset(String id, Handler<AsyncResult<JsonObject>> result);

}
