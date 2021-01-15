package io.piveau.odbl.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

public class DatasetCollection extends BaseModel {

    public String id;
    public JsonArray datasets;
    public String issuer;
    public Instant issued;
    public Instant updated;
    public String status;

    public DatasetCollection(String id, JsonArray datasets, String issuer, Instant issued, Instant updated, String status) {
        this.id = id;
        this.datasets = datasets;
        this.issuer = issuer;
        this.issued = issued;
        this.updated = updated;
        this.status = status;
    }

    public static DatasetCollection fromJson(JsonObject jsonObject) {
        return new DatasetCollection(
            jsonObject.getString("id"),
            jsonObject.getJsonArray("datasets"),
            jsonObject.getString("issuer"),
            jsonObject.getInstant("issued"),
            jsonObject.getInstant("updated"),
            jsonObject.getString("status")
        );
    }
}
