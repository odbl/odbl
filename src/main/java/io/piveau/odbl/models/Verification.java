package io.piveau.odbl.models;

import io.vertx.core.json.JsonObject;

public class Verification extends BaseModel {

    public String issuer;
    public String hash;
    public String signature;


    public Verification(String issuer, String hash, String signature) {
        this.issuer = issuer;
        this.hash = hash;
        this.signature = signature;
    }

    public static Verification fromJson(JsonObject jsonObject) {
        return new Verification(
            jsonObject.getString("issuer"),
            jsonObject.getString("hash"),
            jsonObject.getString("signature")
        );
    }
}
