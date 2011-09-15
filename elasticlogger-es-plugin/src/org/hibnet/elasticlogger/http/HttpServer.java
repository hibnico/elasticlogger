/*
 * Licensed to Nicolas Lalevée under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Server;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.BindHttpException;

public class HttpServer extends AbstractLifecycleComponent<HttpServer> {

    private final NetworkService networkService;

    private int port;
    private String bindHost;
    private String publishHost;

    private Server server;

    private final Client client;

    @Inject
    public HttpServer(Settings settings, NetworkService networkService, Client client) {
        super(settings);
        this.networkService = networkService;
        this.client = client;
        this.port = Integer.parseInt(componentSettings.get("port", settings.get("http.port", "9800")));
        this.bindHost = componentSettings.get("bind_host", settings.get("http.bind_host", settings.get("http.host")));
        this.publishHost = componentSettings.get("publish_host",
                settings.get("http.publish_host", settings.get("http.host")));
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        InetAddress hostAddress;
        try {
            hostAddress = networkService.resolveBindHostAddress(bindHost);
        } catch (IOException e) {
            throw new BindHttpException("Failed to resolve host [" + bindHost + "]", e);
        }

        InetSocketAddress bindAdress = new InetSocketAddress(hostAddress, port);
        server = new Server(bindAdress);

        server.setHandler(new DispatcherHandler(client));

        try {
            server.start();
        } catch (Exception e) {
            throw new ElasticSearchException("Failed to start the Jetty server", e);
        }

        logger.info("ElasticLogger webapp listening to {}", bindAdress);
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new ElasticSearchException("Failed to stop the Jetty server", e);
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

}
