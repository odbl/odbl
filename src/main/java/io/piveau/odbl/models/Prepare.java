package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class Prepare extends BaseModel {

    public String blockHash;
    public Long timestamp;
    public String transactionId;

    public Prepare(String blockHash, Long timestamp, String transactionId) {
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
    }

    public static Prepare fromJson(JsonObject jsonObject) {
        return new Prepare(
            jsonObject.getString("blockHash"),
            jsonObject.getLong("timestamp"),
            jsonObject.getString("transactionId")
        );
    }

}
