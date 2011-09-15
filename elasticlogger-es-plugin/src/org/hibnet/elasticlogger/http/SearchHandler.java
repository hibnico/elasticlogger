package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class SearchHandler extends AbstractHandler {

    private final TemplateRenderer templateRenderer;
    private final Client client;

    public SearchHandler(Client client, TemplateRenderer templateRenderer) {
        this.client = client;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String[] matched = URIMatcher.getMatched(request);
        String indexName = matched[1];

        int limit = getIntParameter(request, "limit", 100);
        int offset = getIntParameter(request, "offset", 100);
        String sort = getStringParameter(request, "sort", "timestamp");
        String query = getStringParameter(request, "query", "");

        QueryBuilder querybuidler;
        if (query.trim().length() == 0) {
            querybuidler = QueryBuilders.matchAllQuery();
        } else {
            querybuidler = QueryBuilders.queryString(query);
        }

        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.from(offset);
        builder.size(limit);
        builder.sort(sort);
        builder.query(querybuidler);

        SearchRequest searchRequest = new SearchRequest(new String[] { indexName }, builder.buildAsBytes());
        SearchResponse searchResponse = client.search(searchRequest).actionGet();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("indexName", indexName);
        vars.put("limit", limit);
        vars.put("offset", offset);
        vars.put("sort", sort);
        vars.put("query", query);
        vars.put("searchResponse", searchResponse);

        templateRenderer.render(baseRequest, response, "search.html", vars);
    }

    private int getIntParameter(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringParameter(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
