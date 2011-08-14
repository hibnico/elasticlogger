package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;

public class DispatcherHandler extends AbstractHandlerContainer {

    private Map<String, Handler> handlers = new LinkedHashMap<String, Handler>();

    public DispatcherHandler() {
        TemplateRenderer templateRenderer = new TemplateRenderer();

        IndexHandler indexHandler = new IndexHandler(templateRenderer);
        handlers.put("/", indexHandler);
        handlers.put("/index.html", indexHandler);
    }

    @Override
    public Handler[] getHandlers() {
        return handlers.values().toArray(new Handler[0]);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        for (Entry<String, Handler> entry : handlers.entrySet()) {
            if (target.startsWith(entry.getKey())) {
                entry.getValue().handle(target, baseRequest, request, response);
                return;
            }
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        baseRequest.setHandled(true);
    }

}
