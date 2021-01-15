package io.piveau.odbl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonObject;

public class ProtocolMessage extends BaseModel {

    static public final String TRANSACTION = "transaction";
    static public final String PRE_PREPARE = "preprepare";
    static public final String PREPARE = "prepare";
    static public final String COMMIT = "commit";
    static public final String VIEW_CHANGE = "viewchange";
    static public final String AUTHORITY_MESSAGE = "authority";

    public String type;
    public JsonObject payload;
    public JsonObject verification;

    private ProtocolMessage(String type, JsonObject payload, JsonObject verification) {
        this.type = type;
        this.payload = payload;
        this.verification = verification;
    }

    public static ProtocolMessage createTransactionMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(TRANSACTION, payload, verification);
    }

    public static ProtocolMessage createPrePrepareMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(PRE_PREPARE, payload, verification);
    }

    public static ProtocolMessage createPrepareMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(PREPARE, payload, verification);
    }

    public static ProtocolMessage createCommitMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(COMMIT, payload, verification);
    }

    public static ProtocolMessage createViewChangeMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(VIEW_CHANGE, payload, verification);
    }

    public static ProtocolMessage createAuthorityMessage(JsonObject payload, JsonObject verification) {
        return new ProtocolMessage(AUTHORITY_MESSAGE, payload, verification);
    }


    @JsonIgnore
    public String getIdentity() {
        switch (type) {
            case TRANSACTION:
                return "Transaction (ID: " + payload.getString("id") + ")";
            case PRE_PREPARE:
                return "Pre-Prepare (BlockHash: " + payload.getString("blockHash") + ")";
            case PREPARE:
                return "Prepare (BlockHash: " + payload.getString("blockHash") + ")";
            case COMMIT:
                return "Commit (BlockHash: " + payload.getString("blockHash") + ")";
            case VIEW_CHANGE:
                return "View-Change (BlockHash: " + payload.getString("blockHash") + ")";
            case AUTHORITY_MESSAGE:
                return "Authority Message";
            default:
                return "No Identity could be found";
        }
    }

    @JsonIgnore
    public String getIssuer() {
        return verification.getString("issuer");
    }


    @JsonIgnore
    public boolean isTransaction() {
        return type.equals(TRANSACTION);
    }

    @JsonIgnore
    public boolean isPrePepare() {
        return type.equals(PRE_PREPARE);
    }

    @JsonIgnore
    public boolean isPrepare() {
        return type.equals(PREPARE);
    }

    @JsonIgnore
    public boolean isCommit() {
        return type.equals(COMMIT);
    }

    @JsonIgnore
    public boolean isViewChange() {
        return type.equals(VIEW_CHANGE);
    }

    @JsonIgnore
    public boolean isAuthority() {
        return type.equals(AUTHORITY_MESSAGE);
    }


    public static ProtocolMessage fromJson(JsonObject jsonObject) {
        return new ProtocolMessage(
            jsonObject.getString("type"),
            jsonObject.getJsonObject("payload"),
            jsonObject.getJsonObject("verification")
        );
    }

}
