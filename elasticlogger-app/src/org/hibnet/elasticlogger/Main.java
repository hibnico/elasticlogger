package org.hibnet.elasticlogger;

import org.elasticsearch.bootstrap.ElasticSearch;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {

	public static void main(String[] args) {
		SLF4JBridgeHandler.install();
		ElasticSearch.main(args);
	}
}
