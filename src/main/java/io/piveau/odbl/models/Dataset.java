package io.piveau.odbl.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;

public class Dataset extends BaseModel {

    public String issuer;
    public String payload;
    public String blockHash;
    public String status;
    public Instant issued;

    public Dataset(String issuer, String payload, String blockHash, String status, Instant issued) {
        this.issuer = issuer;
        this.payload = payload;
        this.blockHash = blockHash;
        this.issued = issued;
        this.status = status;

    }

    public static Dataset fromJson(JsonObject jsonObject) {
        return new Dataset(
            jsonObject.getString("issuer"),
            jsonObject.getString("payload"),
            jsonObject.getString("blockHash"),
            jsonObject.getString("status"),
            jsonObject.getInstant("issued")
        );
    }
}
