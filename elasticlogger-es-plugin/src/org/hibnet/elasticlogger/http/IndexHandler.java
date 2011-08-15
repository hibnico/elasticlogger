package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.admin.indices.status.TransportIndicesStatusAction;

public class IndexHandler extends AbstractHandler {

    private final TemplateRenderer templateRenderer;
    private final TransportIndicesStatusAction transportIndicesStatusAction;

    public IndexHandler(TransportIndicesStatusAction transportIndicesStatusAction, TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
        this.transportIndicesStatusAction = transportIndicesStatusAction;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        IndicesStatusRequest indicesStatusRequest = new IndicesStatusRequest();
        IndicesStatusResponse indicesStatusResponse = transportIndicesStatusAction.execute(indicesStatusRequest)
                .actionGet();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("indicesStatusResponse", indicesStatusResponse);

        templateRenderer.render(baseRequest, response, "index.html", vars);
    }

}
