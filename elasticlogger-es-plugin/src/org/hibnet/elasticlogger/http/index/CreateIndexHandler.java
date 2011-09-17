package org.hibnet.elasticlogger.http.index;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.Client;
import org.hibnet.elasticlogger.http.AbstractJsonHandler;
import org.hibnet.elasticlogger.http.JsonResult;

public class CreateIndexHandler extends AbstractJsonHandler<CreateIndexInput, JsonResult> {

    private final Client client;

    private HashMap<String, Object> defaultMapping;

    public CreateIndexHandler(Client client) {
        super(CreateIndexInput.class);
        this.client = client;
        loadDefaultMapping();
    }

    private void loadDefaultMapping() {
        defaultMapping = new HashMap<String, Object>();
        Map<String, Object> propMappings = new HashMap<String, Object>();
        addPropMapping(propMappings, "level", "string", true, false);
        addPropMapping(propMappings, "message", "string", true, true);
        addPropMapping(propMappings, "marker", "string", true, false);
        addPropMapping(propMappings, "loggername", "string", true, true);
        addPropMapping(propMappings, "threadname", "string", true, true);
        addPropMapping(propMappings, "timestamp", "long", true, false);
        addPropMapping(propMappings, "stackTrace", "string", true, true);
        defaultMapping.put("properties", propMappings);
        defaultMapping.put("_source", Collections.singletonMap("enable", false));
    }

    private void addPropMapping(Map<String, Object> propMappings, String name, String type, boolean store,
            boolean analyzed) {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("type", type);
        mapping.put("store", store);
        mapping.put("index", analyzed ? "analyzed" : "not_analyzed");
        propMappings.put(name, mapping);
    }

    @Override
    protected JsonResult handle(CreateIndexInput input, String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (input == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (input.name == null || input.name.trim().length() == 0) {
            return new JsonResult("Invalid name");
        }
        if (input.type == null || input.type.trim().length() == 0) {
            return new JsonResult("Invalid type");
        }
        CreateIndexRequest esRequest = new CreateIndexRequest(input.name);
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put(input.type, defaultMapping);
        esRequest.mapping(input.type, mapping);
        try {
            client.admin().indices().create(esRequest).actionGet();
        } catch (ElasticSearchException e) {
            return new JsonResult(e);
        }
        return JsonResult.OK;
    }
}
