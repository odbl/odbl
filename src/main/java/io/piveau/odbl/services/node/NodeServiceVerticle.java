package io.piveau.odbl.services.node;

import io.piveau.odbl.persistence.MongoManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeServiceVerticle extends AbstractVerticle {

    Logger LOGGER = LoggerFactory.getLogger(NodeServiceVerticle.class);

    private final MongoManager mongoManager;
    private JsonObject config;


    public NodeServiceVerticle(MongoManager mongoManager, JsonObject config) {
        this.mongoManager = mongoManager;
        this.config = config;
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        NodeService.create(vertx, config, mongoManager, ar -> {
            if(ar.succeeded()) {
                new ServiceBinder(vertx).setAddress(NodeService.SERVICE_ADDRESS).register(NodeService.class, ar.result());
                startPromise.complete();
                LOGGER.info("Deployed NodeServiceVerticle");
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

}
