package org.hibnet.elasticlogger.http;

import org.apache.commons.lang.exception.ExceptionUtils;

public class JsonResult {

    public static final JsonResult OK = new JsonResult();

    public boolean ok;

    public String error;

    public String stacktrace;

    public JsonResult() {
        ok = true;
    }

    public JsonResult(String error) {
        ok = false;
        this.error = error;
    }

    public JsonResult(Throwable t) {
        ok = false;
        this.error = t.getMessage();
        this.stacktrace = ExceptionUtils.getFullStackTrace(t);
    }
}
