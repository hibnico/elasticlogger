package org.hibnet.elasticlogger.http;

import static org.hibnet.elasticlogger.http.URIMatcher.endsWith;
import static org.hibnet.elasticlogger.http.URIMatcher.eq;
import static org.hibnet.elasticlogger.http.URIMatcher.regexp;

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
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.elasticsearch.client.Client;
import org.hibnet.elasticlogger.http.index.CreateIndexHandler;
import org.hibnet.elasticlogger.http.index.DropIndexHandler;
import org.hibnet.elasticlogger.http.index.IndexHandler;
import org.hibnet.elasticlogger.http.logs.LogsHandler;

public class DispatcherHandler extends AbstractHandlerContainer {

    // in development, reload every time
    private static final boolean RELOAD_ON_EACH_REQUEST = true;

    private Map<URIMatcher, Handler> handlers = new LinkedHashMap<URIMatcher, Handler>();
    private final Client client;

    public DispatcherHandler(Client client) {
        this.client = client;
        handlers = loadHandlers();
    }

    private Map<URIMatcher, Handler> loadHandlers() {
        Map<URIMatcher, Handler> newHandlers = new LinkedHashMap<URIMatcher, Handler>();

        TemplateRenderer templateRenderer = new TemplateRenderer();
        IndexHandler indexHandler = new IndexHandler(client, templateRenderer);
        newHandlers.put(eq("/"), indexHandler);
        newHandlers.put(eq("/index.html"), indexHandler);
        newHandlers.put(eq("/createIndex"), new CreateIndexHandler(client));
        newHandlers.put(eq("/dropIndex"), new DropIndexHandler(client));

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/org/hibnet/elasticlogger/http/resources/"));
        newHandlers.put(endsWith(".css"), resourceHandler);
        newHandlers.put(endsWith(".png"), resourceHandler);
        newHandlers.put(endsWith(".js"), resourceHandler);

        LogsHandler searchHandler = new LogsHandler(client, templateRenderer);
        newHandlers.put(regexp("/([a-zA-Z][a-zA-Z0-9]*)/"), searchHandler);
        newHandlers.put(regexp("/([a-zA-Z][a-zA-Z0-9]*)/index\\.html"), searchHandler);

        return newHandlers;
    }

    @Override
    public Handler[] getHandlers() {
        return handlers.values().toArray(new Handler[0]);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (RELOAD_ON_EACH_REQUEST) {
            handlers = loadHandlers();
        }
        for (Entry<URIMatcher, Handler> entry : handlers.entrySet()) {
            if (entry.getKey().match(target, request)) {
                entry.getValue().handle(target, baseRequest, request, response);
                return;
            }
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        baseRequest.setHandled(true);
    }

}
