package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public abstract class BaseModel {

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}
