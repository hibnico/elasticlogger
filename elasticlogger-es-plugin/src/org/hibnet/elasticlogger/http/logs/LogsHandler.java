package org.hibnet.elasticlogger.http.logs;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.hibnet.elasticlogger.http.TemplateRenderer;
import org.hibnet.elasticlogger.http.URIMatcher;

public class LogsHandler extends AbstractHandler {

    private final TemplateRenderer templateRenderer;

    public LogsHandler(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String[] matched = URIMatcher.getMatched(request);
        String indexName = matched[1];

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("indexName", indexName);

        addBreadcrumb(vars, "/" + indexName + "/", "Index '" + indexName + "'");

        templateRenderer.render(baseRequest, response, "logs/logs.html", vars);
    }

    private void addBreadcrumb(Map<String, Object> vars, String... breadcrumb) {
        Map<String, String> breadcrumbs = new LinkedHashMap<String, String>();
        for (int i = 0; i < breadcrumb.length / 2; i++) {
            breadcrumbs.put(breadcrumb[2 * i], breadcrumb[2 * i + 1]);
        }
        vars.put("breadcrumbs", breadcrumbs);
    }
}
