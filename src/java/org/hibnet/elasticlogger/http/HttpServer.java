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

import static org.elasticsearch.common.network.NetworkService.TcpSettings.TCP_BLOCKING;
import static org.elasticsearch.common.network.NetworkService.TcpSettings.TCP_BLOCKING_SERVER;
import static org.elasticsearch.common.util.concurrent.EsExecutors.daemonThreadFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.netty.OpenChannelsHandler;
import org.elasticsearch.common.netty.bootstrap.ServerBootstrap;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ChannelPipeline;
import org.elasticsearch.common.netty.channel.ChannelPipelineFactory;
import org.elasticsearch.common.netty.channel.Channels;
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.elasticsearch.common.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.elasticsearch.common.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.elasticsearch.common.netty.handler.codec.http.HttpChunkAggregator;
import org.elasticsearch.common.netty.handler.codec.http.HttpContentCompressor;
import org.elasticsearch.common.netty.handler.codec.http.HttpContentDecompressor;
import org.elasticsearch.common.netty.handler.codec.http.HttpRequestDecoder;
import org.elasticsearch.common.netty.handler.codec.http.HttpResponseEncoder;
import org.elasticsearch.common.netty.handler.timeout.ReadTimeoutException;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.NetworkExceptionHelper;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.transport.BindTransportException;

public class HttpServer extends AbstractLifecycleComponent<HttpServer> {

    private final NetworkService networkService;

    private int workerCount;
    private boolean compression;
    private int compressionLevel;
    private String port;
    private String bindHost;
    private String publishHost;
    private ByteSizeValue maxContentLength;
    private boolean blockingServer;

    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;

    private OpenChannelsHandler serverOpenChannels;

    @Inject
    public HttpServer(Settings settings, NetworkService networkService) {
        super(settings);
        this.networkService = networkService;
        this.blockingServer = settings.getAsBoolean("http.blocking_server",
                settings.getAsBoolean(TCP_BLOCKING_SERVER, settings.getAsBoolean(TCP_BLOCKING, false)));
        this.port = componentSettings.get("port", settings.get("http.port", "9200-9300"));
        this.bindHost = componentSettings.get("bind_host", settings.get("http.bind_host", settings.get("http.host")));
        this.publishHost = componentSettings.get("publish_host",
                settings.get("http.publish_host", settings.get("http.host")));
        ByteSizeValue maxContentLength = componentSettings.getAsBytesSize("max_content_length",
                settings.getAsBytesSize("http.max_content_length", new ByteSizeValue(100, ByteSizeUnit.MB)));
        this.workerCount = componentSettings.getAsInt("worker_count", Runtime.getRuntime().availableProcessors() * 2);
        this.compression = settings.getAsBoolean("http.compression", true);
        this.compressionLevel = settings.getAsInt("http.compression_level", 6);

        // validate max content length
        if (maxContentLength.bytes() > Integer.MAX_VALUE) {
            logger.warn("maxContentLength[" + maxContentLength + "] set to high value, resetting it to [100mb]");
            maxContentLength = new ByteSizeValue(100, ByteSizeUnit.MB);
        }
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        serverOpenChannels = new OpenChannelsHandler();

        if (blockingServer) {
            serverBootstrap = new ServerBootstrap(new OioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(daemonThreadFactory(settings, "http_server_boss")),
                    Executors.newCachedThreadPool(daemonThreadFactory(settings, "http_server_worker"))));
        } else {
            serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(daemonThreadFactory(settings, "http_server_boss")),
                    Executors.newCachedThreadPool(daemonThreadFactory(settings, "http_server_worker")), workerCount));
        }

        final HttpRequestHandler requestHandler = new HttpRequestHandler(this);

        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("openChannels", serverOpenChannels);
                pipeline.addLast("decoder", new HttpRequestDecoder());
                if (compression) {
                    pipeline.addLast("decoder_compress", new HttpContentDecompressor());
                }
                pipeline.addLast("aggregator", new HttpChunkAggregator((int) maxContentLength.bytes()));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                if (compression) {
                    pipeline.addLast("encoder_compress", new HttpContentCompressor(compressionLevel));
                }
                pipeline.addLast("handler", requestHandler);
                return pipeline;
            }
        };

        serverBootstrap.setPipelineFactory(pipelineFactory);
        // Bind and start to accept incoming connections.
        InetAddress hostAddressX;
        try {
            hostAddressX = networkService.resolveBindHostAddress(bindHost);
        } catch (IOException e) {
            throw new BindHttpException("Failed to resolve host [" + bindHost + "]", e);
        }
        final InetAddress hostAddress = hostAddressX;

        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<Exception>();
        boolean success = portsRange.iterate(new PortsRange.PortCallback() {
            @Override
            public boolean onPortNumber(int portNumber) {
                try {
                    serverChannel = serverBootstrap.bind(new InetSocketAddress(hostAddress, portNumber));
                } catch (Exception e) {
                    lastException.set(e);
                    return false;
                }
                return true;
            }
        });
        if (!success) {
            throw new BindHttpException("Failed to bind to [" + port + "]", lastException.get());
        }

        InetSocketAddress boundAddress = (InetSocketAddress) serverChannel.getLocalAddress();
        InetSocketAddress publishAddress;
        try {
            publishAddress = new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost),
                    boundAddress.getPort());
        } catch (Exception e) {
            throw new BindTransportException("Failed to resolve publish address", e);
        }
        BoundTransportAddress address = new BoundTransportAddress(new InetSocketTransportAddress(boundAddress),
                new InetSocketTransportAddress(publishAddress));
        if (logger.isInfoEnabled()) {
            logger.info("{}", address);
        }
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
            serverChannel = null;
        }

        if (serverOpenChannels != null) {
            serverOpenChannels.close();
            serverOpenChannels = null;
        }

        if (serverBootstrap != null) {
            serverBootstrap.releaseExternalResources();
            serverBootstrap = null;
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            if (logger.isTraceEnabled()) {
                logger.trace("Connection timeout [{}]", ctx.getChannel().getRemoteAddress());
            }
            ctx.getChannel().close();
        } else {
            if (!lifecycle.started()) {
                // ignore
                return;
            }
            if (!NetworkExceptionHelper.isCloseConnectionException(e.getCause())) {
                logger.warn("Caught exception while handling client http traffic, closing connection", e.getCause());
                ctx.getChannel().disconnect();
            }
        }
    }
}
