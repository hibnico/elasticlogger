package org.hibnet.elasticlogger;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

public class SimpleLogger {

    private org.slf4j.Logger logger;

    public SimpleLogger(String name) {
        logger = LoggerFactory.getLogger(name);
    }

    public SimpleLogger(Class< ? > cl) {
        logger = LoggerFactory.getLogger(cl);
    }

    public void error(Object... msgs) {
        if (logger.isErrorEnabled()) {
            logger.error(concat(msgs), getThrowable(msgs));
        }
    }

    public void warn(Object... msgs) {
        if (logger.isWarnEnabled()) {
            logger.warn(concat(msgs), getThrowable(msgs));
        }
    }

    public void info(Object... msgs) {
        if (logger.isInfoEnabled()) {
            logger.info(concat(msgs), getThrowable(msgs));
        }
    }

    public void debug(Object... msgs) {
        if (logger.isDebugEnabled()) {
            logger.debug(concat(msgs), getThrowable(msgs));
        }
    }

    public void trace(Object... msgs) {
        if (logger.isTraceEnabled()) {
            logger.trace(concat(msgs), getThrowable(msgs));
        }
    }

    private Throwable getThrowable(Object... msgs) {
        if (msgs[msgs.length - 1] instanceof Throwable) {
            return (Throwable) msgs[msgs.length - 1];
        }
        return null;
    }

    private String concat(Object... msgs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            if (i == msgs.length - 1 && msgs[i] instanceof Throwable) {
                break;
            }
            if (msgs[i] == null) {
                builder.append("null");
            } else if (msgs[i].getClass().isArray()) {
                builder.append(Arrays.deepToString(new Object[] {msgs[i]}));
            } else {
                builder.append(msgs[i].toString());
            }
        }
        return builder.toString();
    }

}
