package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class Commit extends BaseModel {

    public String blockHash;
    public Long timestamp;
    public String transactionId;

    public Commit(String blockHash, Long timestamp, String transactionId) {
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
    }

    public static Commit fromJson(JsonObject jsonObject) {
        return new Commit(
            jsonObject.getString("blockHash"),
            jsonObject.getLong("timestamp"),
            jsonObject.getString("transactionId")
        );
    }
}
