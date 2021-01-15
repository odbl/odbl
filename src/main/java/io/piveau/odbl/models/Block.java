package io.piveau.odbl.models;

import io.piveau.odbl.Utils;
import io.vertx.core.json.JsonObject;

public class Block extends BaseModel {

    public Long index;
    public Long timestamp;
    public String prevHash;
    public String hash;
    public String data;
    public String dataHash;
    public String issuer;
    public String signature;

    public Block(Long index, Long timestamp, String prevHash, String hash, String data, String dataHash, String issuer, String signature) {
        this.index = index;
        this.timestamp = timestamp;
        this.prevHash = prevHash;
        this.hash = hash;
        this.data = data;
        this.dataHash = dataHash;
        this.issuer = issuer;
        this.signature = signature;
    }


    public static Block fromJson(JsonObject jsonObject) {
        return new Block(
            jsonObject.getLong("index"),
            jsonObject.getLong("timestamp"),
            jsonObject.getString("prevHash"),
            jsonObject.getString("hash"),
            jsonObject.getString("data"),
            jsonObject.getString("dataHash"),
            jsonObject.getString("issuer"),
            jsonObject.getString("signature")
        );
    }

    public static Block createGenesisBlock() {
        return new Block(
            0L,
            Utils.getCurrentTimestamp(),
            null,
            "genesis",
            null,
            null,
            null,
            null
        );
    }

}
