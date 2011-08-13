package org.hibnet.elasticlogger.http;

import java.util.Map;

import org.elasticsearch.common.netty.channel.Channel;

public interface TemplateRenderer {

    public void render(String template, Map<String, String> vars, Channel channel);
}
