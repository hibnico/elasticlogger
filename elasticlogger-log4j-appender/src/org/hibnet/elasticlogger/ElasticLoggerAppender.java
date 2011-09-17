package org.hibnet.elasticlogger;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;

public class ElasticLoggerAppender extends AppenderSkeleton {

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
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void activateOptions() {
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
    }

    @Override
    public void close() {
        if (node != null) {
            node.close();
        }
    }

    @Override
    protected void append(LoggingEvent event) {
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
            if (event.getThrowableStrRep() != null) {
                jsonBuilder.field("stacktrace", Arrays.toString(event.getThrowableStrRep()));
            }
        } catch (IOException e) {
            System.err.println("Error while writing json: " + e.getMessage());
            return;
        }

        if (client == null) { // the client maybe not be ready yet
            boolean added = queue.add(jsonBuilder);
            if (!added) {
                System.err.println("ElasticLoggerAppender queue is full, too much events while waiting the"
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

}
