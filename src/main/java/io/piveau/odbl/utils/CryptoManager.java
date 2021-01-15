package io.piveau.odbl.utils;

import io.piveau.odbl.models.Block;
import io.piveau.odbl.services.protocol.ProtocolUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * https://docs.oracle.com/javase/7/docs/api/java/security/package-summary.html
 * https://docs.oracle.com/javase/7/docs/api/javax/crypto/package-summary.html
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator
 */
public class CryptoManager {

    Logger LOGGER = LoggerFactory.getLogger(CryptoManager.class);

    public PublicKey publicKey;
    public PrivateKey privateKey;
    private KeyPair keyPair;

    public static CryptoManager fromContext(Vertx vertx) {
        JsonObject keys = NetworkEnvironment.getKeys(vertx).getJsonArray("keys").getJsonObject(0);
        return new CryptoManager(keys.getString("publicKey"), keys.getString("privateKey"));
    }

    public CryptoManager(String publicKey, String privateKey) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));

            this.privateKey = kf.generatePrivate(keySpecPKCS8);
            this.publicKey = kf.generatePublic(keySpecX509);
            this.keyPair = new KeyPair(this.publicKey, this.privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public CryptoManager() { }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            this.keyPair = keyPairGenerator.generateKeyPair();;
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static String getHashForBlock(Block block) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
            .append(block.index)
            .append(block.data)
            .append(block.dataHash)
            .append(block.issuer)
            .append(block.prevHash)
            .append(block.timestamp);
        return DigestUtils.sha256Hex(stringBuilder.toString());
    }


    public static boolean validateBlockHash(Block block, String hash) {
        return getHashForBlock(block).equals(hash);
    }

    public String sign(String message) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(message.getBytes());
            byte[] signature = sign.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public static String hashJson(JsonObject json) {
//        String encoded = json.encode();
//        char[] tempArray = encoded.toCharArray();
//        Arrays.sort(tempArray);
//        String sorted = new String(tempArray);
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] encodedhash = digest.digest(sorted.getBytes(StandardCharsets.UTF_8));
//            return Base64.getEncoder().encodeToString(encodedhash);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//            return null;
//        }
        return String.valueOf(json.hashCode());
    }

    public static boolean verify(String message, String publicKey, String signature) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            sign.initVerify(kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))));
            sign.update(message.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sign.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            return false;
        }
    }


    public boolean verify(String message, String signature) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(publicKey);
            sign.update(message.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sign.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String getPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }


}
