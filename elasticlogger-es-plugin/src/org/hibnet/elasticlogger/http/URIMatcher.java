package org.hibnet.elasticlogger.http;

import java.util.regex.Pattern;

public abstract class URIMatcher {

    public abstract boolean match(String uri);

    public static URIMatcher eq(final String expected) {
        return new URIMatcher() {
            @Override
            public boolean match(String uri) {
                return expected.equals(uri);
            }
            @Override
            public String toString() {
                return "eq(" + expected + ")";
            }
        };
    }

    public static URIMatcher startWith(final String expected) {
        return new URIMatcher() {
            @Override
            public boolean match(String uri) {
                return uri.startsWith(expected);
            }
            @Override
            public String toString() {
                return "startWith(" + expected + ")";
            }
        };
    }

    public static URIMatcher endsWith(final String expected) {
        return new URIMatcher() {
            @Override
            public boolean match(String uri) {
                return uri.endsWith(expected);
            }
            @Override
            public String toString() {
                return "endsWith(" + expected + ")";
            }
        };
    }

    public static URIMatcher regexp(final String regexp) {
        final Pattern p = Pattern.compile(regexp);
        return new URIMatcher() {
            @Override
            public boolean match(String uri) {
                return p.matcher(uri).matches();
            }
            @Override
            public String toString() {
                return "regexp(" + regexp + ")";
            }
        };
    }

}
