package org.hibnet.elasticlogger.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public abstract class URIMatcher {

    private static final String REQATT_MATCHED = "URIMatcher_matched";

    public abstract boolean match(String uri, HttpServletRequest request);

    public static URIMatcher eq(final String expected) {
        return new URIMatcher() {
            @Override
            public boolean match(String uri, HttpServletRequest request) {
                boolean equals = expected.equals(uri);
                if (equals) {
                    setMatched(request, expected);
                }
                return equals;
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
            public boolean match(String uri, HttpServletRequest request) {
                boolean startsWith = uri.startsWith(expected);
                if (startsWith) {
                    setMatched(request, uri, expected, uri.substring(expected.length()));
                }
                return startsWith;
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
            public boolean match(String uri, HttpServletRequest request) {
                boolean endsWith = uri.endsWith(expected);
                if (endsWith) {
                    setMatched(request, uri, uri.substring(0, uri.length() - expected.length()), expected);
                }
                return endsWith;
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
            public boolean match(String uri, HttpServletRequest request) {
                Matcher matcher = p.matcher(uri);
                if (matcher.matches()) {
                    String[] matched = new String[matcher.groupCount() + 1];
                    for (int i = 0; i < matched.length; i++) {
                        matched[i] = matcher.group(i);
                    }
                    setMatched(request, matched);
                }
                return matcher.matches();
            }

            @Override
            public String toString() {
                return "regexp(" + regexp + ")";
            }
        };
    }

    private static void setMatched(HttpServletRequest request, String... matched) {
        request.setAttribute(REQATT_MATCHED, matched);
    }

    public static String[] getMatched(HttpServletRequest request) {
        return (String[]) request.getAttribute(REQATT_MATCHED);
    }
}
