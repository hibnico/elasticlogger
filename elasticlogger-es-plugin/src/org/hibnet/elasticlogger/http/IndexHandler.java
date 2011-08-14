package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class IndexHandler extends AbstractHandler {

    private final TemplateRenderer templateRenderer;

    public IndexHandler(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Map<String, Object> vars = null;

        templateRenderer.render(baseRequest, response, "index.html", vars);
    }

}
