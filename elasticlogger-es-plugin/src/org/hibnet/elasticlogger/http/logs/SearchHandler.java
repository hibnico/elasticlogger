package org.hibnet.elasticlogger.http.logs;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.elasticsearch.search.sort.SortOrder;
import org.hibnet.elasticlogger.SimpleLogger;
import org.hibnet.elasticlogger.http.URIMatcher;

public class SearchHandler extends WebSocketHandler {

    private static SimpleLogger logger = new SimpleLogger(SearchHandler.class);

    private ObjectMapper mapper = new ObjectMapper();

    private final Client client;

    public SearchHandler(Client client) {
        this.client = client;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        String[] matched = URIMatcher.getMatched(request);
        String indexName = matched[1];
        return new SearchWebSocket(indexName);
    }

    class SearchWebSocket implements OnTextMessage {

        private Connection connection;

        private final String indexName;

        public SearchWebSocket(String indexName) {
            this.indexName = indexName;
        }

        @Override
        public void onOpen(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void onClose(int closeCode, String message) {
            // nothing to do
        }

        @Override
        public void onMessage(String data) {
            SearchQuery query;
            try {
                query = mapper.readValue(data, SearchQuery.class);
            } catch (Exception e) {
                logger.error("Unable to read the query from the websocket: '", data, "'", e);
                return;
            }
            try {
                XContentBuilder response = search(indexName, query);
                connection.sendMessage(response.string());
            } catch (IOException e) {
                logger.error("Unable to send response of the search query", e);
            }
        }

    }

    private XContentBuilder search(String indexName, SearchQuery inputQuery) throws IOException {
        QueryBuilder query;
        if (inputQuery.query == null || inputQuery.query.trim().length() == 0) {
            query = QueryBuilders.matchAllQuery();
        } else {
            query = QueryBuilders.boolQuery();
            ((BoolQueryBuilder) query).should(QueryBuilders.textPhrasePrefixQuery("message", inputQuery.query));
            ((BoolQueryBuilder) query).should(QueryBuilders.textPhrasePrefixQuery("loggername", inputQuery.query));
            ((BoolQueryBuilder) query).should(QueryBuilders.textPhrasePrefixQuery("threadname", inputQuery.query));
        }

        SearchRequestBuilder search = client.prepareSearch(indexName);
        search.setQuery(query);
        search.setSize(20);
        search.addSort("timestamp", SortOrder.DESC);
        search.addFacet(FacetBuilders.termsFacet("level").field("level"));
        search.addFacet(FacetBuilders.termsFacet("threadname").field("threadname").size(5).order(ComparatorType.COUNT));
        search.addFacet(FacetBuilders.termsFacet("loggername").field("loggername").size(5).order(ComparatorType.COUNT));
        search.addFacet(FacetBuilders.dateHistogramFacet("datehistogram").field("timestamp").interval("minute"));
        SearchResponse response = search.execute().actionGet();
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        response.toXContent(builder, null);
        builder.endObject();
        return builder;
    }
}
