package org.hibnet.elasticlogger;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class ElasticLoggerAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String clusterName = "elasticsearch";

    private String indexName = "elasticlogger";

    private String indexType = "log";

    private int queueSize = 1000;

    private Node node;

    private volatile Client client;

    private Queue<XContentBuilder> queue;

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    @Override
    public void start() {
        queue = new LinkedBlockingQueue<XContentBuilder>(queueSize);

        // do make the make the logger (and probably the application too) wait for elasticsearch to boot
        new Thread(new Runnable() {
            @Override
            public void run() {
                node = nodeBuilder().client(true).clusterName(clusterName).node();
                client = node.client();
                // now that the client has started, empty the buffered events
                while (!queue.isEmpty()) {
                    doIndex(queue.poll());
                }
                // Note : there may be some very edge case where the queue might be still not empty here, but it seems
                // very unlikely, and we accept here to lost these events to avoid too much synchronization between the
                // threads
            }
        }, "ElasticloggerAppender node client starter").start();

        super.start();
    }

    @Override
    public void stop() {
        if (node != null) {
            node.close();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        XContentBuilder jsonBuilder;
        try {
            jsonBuilder = XContentFactory.jsonBuilder();
            jsonBuilder.startObject();
            jsonBuilder.field("timestamp", Long.toString(event.getTimeStamp()));
            if (event.getLevel() != null) {
                jsonBuilder.field("level", event.getLevel());
            }
            if (event.getMessage() != null) {
                jsonBuilder.field("message", event.getMessage());
            }
            if (event.getLoggerName() != null) {
                jsonBuilder.field("loggername", event.getLoggerName());
            }
            if (event.getThreadName() != null) {
                jsonBuilder.field("threadname", event.getThreadName());
            }
            if (event.getMarker() != null) {
                jsonBuilder.field("marker", event.getMarker());
            }
            if (event.getThrowableProxy() != null) {
                jsonBuilder.field("stacktrace", ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }
            if (event.getMdc() != null) {
                for (Entry<String, String> mdcEntry : event.getMdc().entrySet()) {
                    jsonBuilder.field("mdc_" + mdcEntry.getKey(), mdcEntry.getValue());
                }
            }
        } catch (IOException e) {
            addError("Error while writing json: " + e.getMessage());
            return;
        }

        if (client == null) { // the client maybe not be ready yet
            boolean added = queue.add(jsonBuilder);
            if (!added) {
                addError("ElasticLoggerAppender queue is full, too much events while waiting the"
                        + " elasticsearch client to boot. Some events are then lost.");
            }
        } else {
            doIndex(jsonBuilder);
        }
    }

    private void doIndex(XContentBuilder json) {
        client.prepareIndex(indexName, indexType).setSource(json).execute();
        // not that we don't wait for the response, but this is nice, we don't want to have the main thread wait for
        // some remote logging indexation
    }

    private static class JsonBuilder {

        private StringBuilder builder = new StringBuilder("{");

        private boolean first = true;

        private void add(String name, Object value) {
            if (value == null) {
                return;
            }
            add(name, value.toString());
        }

        private void add(String name, String value) {
            if (value == null || value.length() == 0) {
                return;
            }
            if (first) {
                builder.append('\"');
            } else {
                builder.append(",\"");
            }
            first = false;
            builder.append(name);
            builder.append("\":\"");
            builder.append(value.replaceAll("\"", "\\\""));
            builder.append('\"');
        }

        private String getResult() {
            builder.append('}');
            return builder.toString();
        }

    }
}
