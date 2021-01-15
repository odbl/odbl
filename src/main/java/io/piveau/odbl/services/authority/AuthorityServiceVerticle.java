package io.piveau.odbl.services.authority;

import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.services.node.NodeService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorityServiceVerticle extends AbstractVerticle {

    Logger LOGGER = LoggerFactory.getLogger(AuthorityServiceVerticle.class);

    private final MongoManager mongoManager;
    private JsonObject config;


    public AuthorityServiceVerticle(MongoManager mongoManager, JsonObject config) {
        this.mongoManager = mongoManager;
        this.config = config;
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        AuthorityService.create(vertx, config, mongoManager, ar -> {
            if(ar.succeeded()) {
                new ServiceBinder(vertx).setAddress(AuthorityService.SERVICE_ADDRESS).register(AuthorityService.class, ar.result());
                startPromise.complete();
                LOGGER.info("Deployed AuthorityServiceVerticle");
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

}
