package io.piveau.odbl.persistence;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class PersistenceFactory {

    private PersistenceFactory() { }

    public static MongoManager getMongoDBManager(Vertx vertx, String host, int port, String dbName) {
        JsonObject config = new JsonObject()
            .put("host", host)
            .put("port", port)
            .put("db_name", dbName);

        MongoClient client = MongoClient.createShared(vertx, config);
        MongoManager mongoManager = new MongoManager(client);
        return mongoManager;
    }

}
