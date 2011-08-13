package org.hibnet.elasticlogger.testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(Main.class);
        while (true) {
            logger.trace("test trace");
            logger.debug("test debug");
            logger.info("test info");
            logger.warn("test warn");
            logger.error("test error");
            Thread.sleep(1000);
        }
    }
}
