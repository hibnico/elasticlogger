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

import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.channel.ChannelFuture;
import org.elasticsearch.common.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.netty.channel.ChannelHandler;
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.elasticsearch.common.netty.channel.MessageEvent;
import org.elasticsearch.common.netty.channel.SimpleChannelUpstreamHandler;
import org.elasticsearch.common.netty.handler.codec.http.HttpRequest;

@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

    private final LoggerHttpServer httpServer;

    private final TemplateRenderer templateRenderer = new TemplateRenderer();

    public HttpRequestHandler(LoggerHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        ChannelBuffer buffer = templateRenderer.render("index.html", null);
        ChannelFuture f = e.getChannel().write(buffer);
        f.addListener(ChannelFutureListener.CLOSE);
        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        httpServer.exceptionCaught(ctx, e);
    }

}
