package io.piveau.odbl;

import io.piveau.odbl.models.Block;
import io.piveau.odbl.utils.CryptoManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Testing Crypto Library")
@ExtendWith(VertxExtension.class)
public class CryptoTest {

    Logger LOGGER = LoggerFactory.getLogger(CryptoTest.class);

    @Test
    void testKeyGeneration(Vertx vertx, VertxTestContext testContext) {
        CryptoManager cryptoManager = new CryptoManager();
        cryptoManager.generateKeyPair();
        LOGGER.info(cryptoManager.getPrivateKeyBase64());
        LOGGER.info(cryptoManager.getPublicKeyBase64());
        String message = "Hello World!";
        String signature = cryptoManager.sign(message);
        LOGGER.info(signature);
        boolean verify = cryptoManager.verify(message, signature);
        assertTrue(verify);

        CryptoManager cryptoManager2 = new CryptoManager();
        cryptoManager2.generateKeyPair();
        String signature2 = cryptoManager2.sign(message);

        boolean verify2 = cryptoManager.verify(message, signature2);
        assertFalse(verify2);

        testContext.completeNow();
    }


    @Test
    void testKeyLoading(Vertx vertx, VertxTestContext testContext) {
        String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQClkBhSHnYbqUfztw35NInVM7ymTUsgKTLrdBFMJSH3+lDpzseg1P3Yl8X/dmwHNgGbYcI9q/kjvlgvoUQKO/dQYTG7uVo10MelSVXPEEnz0htnruc0/MH21sH09rerzdpRyEY0Iaxh1wvswCJvAZD9fK0/rg5nh+PInvqE9sh5Il4f+5qhbj2oVHEEDtPnjCUnWf1nQEH62/+ghGqd9pEYdOZ+C3+REor3UIJVoeuVNFhXp+b+IkyREChFeCObCsENi2y6dfi+PFyWLL2Q62PAn/epXdyoMR+qeuRJLR8R5KqOvDmLWwMoOPTGPe7Ua0U6CEBckCAIpBNiGo1VNdNjAgMBAAECggEBAIkEzfv4VcriuDeNAbWCs6eM383KHiiJsCiIsGASQyCVOy2lAjWq0ELWqPiZKyJ0obPUngqoLtJUD2urzUGjCzSsm5o+9def5/p0zN1HH8z3z9JpP9PscyIz7eUr4in/fXU9iExQqfd3H9lZ0aWI6FPBQSh4hldMcUlxuOcdzh/g6ryn84jVxFmrXhoSN3AM9YkbzfhSPtsDiFmQVItbUy68Qn81A9TrFWY4ocq8F8Zfa6OE2+Lt9yqKYb3tMugq1+7O4igsUnTvTB7vmKSbuJEMlJ34wgDwVhXet53xEl/0PHHkmam9GxrFewjHMHnA3H/4W8AZNpzIYg5wtDMkzMECgYEAzpfQUeeS0nA4apbQKjmEj+848BzAaWuw5SCHNJ+RHqd8s5cSTzCm4yCE9kLlSf+mCTN0EPG30mKe7KIM7FpS8ZEQPQ5YxK2HyNFeOCJ5FOmvWvOSvZHMlBXw5gt6ONRWXj3pQR2ihIsCCVBtuWGcx4mY56iYtA7NhSgOJHCKRlECgYEAzShNy52Y8AgNacskR9plKY6hOxyQXp5RSnMIzXVBHq5U05cfsHd1fY+liGAxCpTtXl95HkFO6ZHO/3eVxFDcgyNf1ZePqxid/fdeN40uWEHI0LFjUhexoF2pR4AzEpC1dldogApOQ4CPznqDaNNEMhJl3sQV0wMfS5ih5/H8LXMCgYAprtnaXLXz/a50Wx9/FYHYpLTBRZvQ6WiUol6FUJiwLazsc1O/ZJqXgw0wvsiSiKFQg/AsJwkmyr00E3I2EfdgRBuZphZlAHpAG3Y+Lz5s6MV3vWNjGlLDojWrxK8RXk5az3ULrVYIFiAADxlEaRx/6hRO0WaWFdKDxlQsKATOMQKBgDXnCAGeh6dRRkzsjby/OeMgUWZZi+kASbV44fAxhYmNHkZ1p2LTEDCMRF6/f0Mbe/5WsVjsqdIeeDeQ8O2inT1rVuukpZ+7mQ84Ji4MTwfrSNrkMIdKKGZNFYPuv/x5vuO39YHms0dzCNWkRNCO6ZUKVm+gL2fAo0FWefuaDbrXAoGAW0NwtpHzOXUyyG3SQ5TlMUgIG2wExmHfXuQojXrUq1NT67cbgOUGi09hR11S2MCzY+rQO5lsuziJ9L33R/cXNCrJA8lhc6DaVQZNVr8WZCQ1ebqBH7h5ccKRJJkyXHfzcWbWWwWxXFded+YlfORVQOpjAnFg+Xg3oX56u1RS47A=";
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApZAYUh52G6lH87cN+TSJ1TO8pk1LICky63QRTCUh9/pQ6c7HoNT92JfF/3ZsBzYBm2HCPav5I75YL6FECjv3UGExu7laNdDHpUlVzxBJ89IbZ67nNPzB9tbB9Pa3q83aUchGNCGsYdcL7MAibwGQ/XytP64OZ4fjyJ76hPbIeSJeH/uaoW49qFRxBA7T54wlJ1n9Z0BB+tv/oIRqnfaRGHTmfgt/kRKK91CCVaHrlTRYV6fm/iJMkRAoRXgjmwrBDYtsunX4vjxcliy9kOtjwJ/3qV3cqDEfqnrkSS0fEeSqjrw5i1sDKDj0xj3u1GtFOghAXJAgCKQTYhqNVTXTYwIDAQAB";
        CryptoManager cryptoManager = new CryptoManager(publicKey, privateKey);
        LOGGER.info(cryptoManager.getPrivateKeyBase64());
        LOGGER.info(cryptoManager.getPublicKeyBase64());
        testContext.completeNow();
    }


    @Test
    void testJSONHash(Vertx vertx, VertxTestContext testContext) {
        JsonObject json = new JsonObject();
        json
            .put("hello", "δεδομένων")
            .put("test", "yeah")
            .put("values", new JsonArray().add("one'").add("two"))
            .put("awesome", new JsonObject().put("2", true));
        String hash = CryptoManager.hashJson(json);
        LOGGER.info(hash);

        JsonObject json2 = new JsonObject();
        json2
            .put("awesome", new JsonObject().put("2", true))
            .put("values", new JsonArray().add("one'").add("two"))
            .put("hello", "δεδομένων")
            .put("test", "yeah");
        String hash2 = CryptoManager.hashJson(json2);
        LOGGER.info(hash2);

        String raw = "{\n" +
            "  \"awesome\" : {\n" +
            "    \"2\" : true\n" +
            "  },\n" +
            "  \"hello\" : \"δεδομένων\",\n" +
            "  \"values\" : [ \"one'\", \"two\" ],\n" +
            "  \"test\" : \"yeah\"\n" +
            "}";
        JsonObject json3 = new JsonObject(raw);
        String hash3 = CryptoManager.hashJson(json3);
        LOGGER.info(hash3);

        assertEquals(hash, hash2);
        assertEquals(hash2, hash3);

        testContext.completeNow();
    }

    @Test
    void testBlockHash(Vertx vertx, VertxTestContext testContext) {
        Block block =  new Block(
            5L,
            1601726329L,
            "f3948t7b4w56798vntw837",
            "cmv6386736vm346m3",
            "784824375b56237563485324ntv378590348n57t",
            "252535435646",
            "node01",
            "f4q3v57345t7234582304"
        );
        String hash = CryptoManager.getHashForBlock(block);
        assertTrue(CryptoManager.validateBlockHash(block, hash));
        testContext.completeNow();
    }

}
