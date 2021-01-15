package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class ViewChange extends BaseModel {

    public String blockHash;
    public Long timestamp;
    public String nextPrimary;
    public String transactionId;

    public ViewChange(String blockHash, Long timestamp, String nextPrimary, String transactionId) {
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.nextPrimary = nextPrimary;
        this.transactionId = transactionId;
    }

    public static ViewChange fromJson(JsonObject jsonObject) {
        return new ViewChange(
            jsonObject.getString("blockHash"),
            jsonObject.getLong("timestamp"),
            jsonObject.getString("nextPrimary"),
            jsonObject.getString("transactionId")
        );
    }
}
