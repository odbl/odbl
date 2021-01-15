package io.piveau.odbl.services.protocol;

import io.piveau.odbl.models.*;
import io.piveau.odbl.utils.CryptoManager;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolMessageFactory {

    Logger LOGGER = LoggerFactory.getLogger(ProtocolMessageFactory.class);

    private Vertx vertx;

    public ProtocolMessageFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public Verification createVerification(JsonObject payload) {
        String hash = CryptoManager.hashJson(payload);
        String issuer = NetworkEnvironment.getMeta(vertx).getString("nodeID");
        String signature = CryptoManager.fromContext(vertx).sign(hash);
        Verification verification =  new Verification(issuer, hash, signature);
        return verification;
    }

    public ProtocolMessage createTransactionMessage(Transaction transaction) {
        JsonObject transactionPayload = transaction.toJson();
        Verification verification = createVerification(transactionPayload);
        return ProtocolMessage.createTransactionMessage(transactionPayload, verification.toJson());
    }

    public ProtocolMessage createPrePrepareMessage(PrePrepare prePrepare) {
        JsonObject prePreparePayload = prePrepare.toJson();
        Verification verification = createVerification(prePreparePayload);
        return ProtocolMessage.createPrePrepareMessage(prePreparePayload, verification.toJson());
    }

    public ProtocolMessage createPrepareMessage(Prepare prepare) {
        JsonObject preparePayload = prepare.toJson();
        Verification verification = createVerification(preparePayload);
        return ProtocolMessage.createPrepareMessage(preparePayload, verification.toJson());
    }

    public ProtocolMessage createCommitMessage(Commit commit) {
        JsonObject commitPayload = commit.toJson();
        Verification verification = createVerification(commitPayload);
        return ProtocolMessage.createCommitMessage(commitPayload, verification.toJson());
    }

    public ProtocolMessage createViewChangeMessage(ViewChange viewChange) {
        JsonObject viewChangePayload = viewChange.toJson();
        Verification verification = createVerification(viewChangePayload);
        return ProtocolMessage.createViewChangeMessage(viewChangePayload, verification.toJson());
    }

    public ProtocolMessage createAuthorityMessage(JsonObject payload) {
        Verification verification = new Verification("authority", "hash", "signature");
        return ProtocolMessage.createAuthorityMessage(payload, verification.toJson());
    }



}
