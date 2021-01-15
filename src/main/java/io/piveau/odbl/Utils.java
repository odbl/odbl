package io.piveau.odbl;

import io.piveau.odbl.models.ProtocolMessage;
import org.slf4j.Logger;

import java.time.Instant;

public class Utils {

    public static Long getCurrentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    public static void prtLog(Logger logger, ProtocolMessage message) {
        if(message.isTransaction()) {
            logger.info("");
        }

    }


}
