package org.hibnet.elasticlogger.http.index;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.hibnet.elasticlogger.http.AbstractJsonHandler;
import org.hibnet.elasticlogger.http.JsonResult;

public class DropIndexHandler extends AbstractJsonHandler<DropIndexInput, JsonResult> {

    private final Client client;

    public DropIndexHandler(Client client) {
        super(DropIndexInput.class);
        this.client = client;
    }

    @Override
    protected JsonResult handle(DropIndexInput input, String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (input == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (input.name == null || input.name.trim().length() == 0) {
            return new JsonResult("Invalid name");
        }
        DeleteIndexRequest esRequest = new DeleteIndexRequest(input.name);
        try {
            client.admin().indices().delete(esRequest).actionGet();
        } catch (ElasticSearchException e) {
            return new JsonResult(e);
        }
        return JsonResult.OK;
    }
}
