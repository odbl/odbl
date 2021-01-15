package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class Transaction extends BaseModel {

    public String id;
    public String payload;
    public String format;
    public Long timestamp;

    public Transaction(String id, String payload, String format, Long timestamp) {
        this.id = id;
        this.payload = payload;
        this.format = format;
        this.timestamp = timestamp;
    }

    public static Transaction fromJson(JsonObject jsonObject) {
        return new Transaction(
            jsonObject.getString("id"),
            jsonObject.getString("payload"),
            jsonObject.getString("format"),
            jsonObject.getLong("timestamp")
        );
    }
}
