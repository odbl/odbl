package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class PrePrepare extends BaseModel {

    static public final String STATE_PREPREPARED = "PREPREPARED";
    static public final String STATE_PREPARED = "PREPARED";
    static public final String STATE_COMMITED = "COMMITED";
    static public final String STATE_FINALCOMMITTED = "FINALCOMMITTED";

    public String state;
    public Long timestamp;
    public String blockHash;
    public JsonObject block;
    public String transactionId;


    public PrePrepare(String state, Long timestamp, String blockHash, JsonObject block, String transactionId) {
        this.state = state;
        this.timestamp = timestamp;
        this.blockHash = blockHash;
        this.block = block;
        this.transactionId = transactionId;
    }

    public static PrePrepare fromJson(JsonObject jsonObject) {
        return new PrePrepare(
            jsonObject.getString("state"),
            jsonObject.getLong("timestamp"),
            jsonObject.getString("blockHash"),
            jsonObject.getJsonObject("block"),
            jsonObject.getString("transactionId")
        );
    }
}
