package io.piveau.odbl.services.admin;

import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.node.NodeService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminServiceVerticle extends AbstractVerticle {

    Logger LOGGER = LoggerFactory.getLogger(AdminServiceVerticle.class);

    private final MongoManager mongoManager;
    private JsonObject config;


    public AdminServiceVerticle(MongoManager mongoManager, JsonObject config) {
        this.mongoManager = mongoManager;
        this.config = config;
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        AdminService.create(vertx, config, mongoManager, ar -> {
            if(ar.succeeded()) {
                new ServiceBinder(vertx).setAddress(AdminService.SERVICE_ADDRESS).register(AdminService.class, ar.result());
                startPromise.complete();
                LOGGER.info("Deployed AdminServiceVerticle");
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

}
