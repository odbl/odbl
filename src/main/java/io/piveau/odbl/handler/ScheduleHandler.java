package io.piveau.odbl.handler;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleHandler extends BaseHandler  {

    Logger LOGGER = LoggerFactory.getLogger(ScheduleHandler.class);

    private Vertx vertx;

    public ScheduleHandler(Vertx vertx) {
        this.vertx = vertx;
    }

    public void healthHandler(Long id) {
        LOGGER.info("Called health handler");
    }

}
