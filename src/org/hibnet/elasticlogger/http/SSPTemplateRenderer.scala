package org.hibnet.elasticlogger.http

import java.util.HashMap

import org.elasticsearch.common.netty.channel.Channel
import org.fusesource.scalate.support.URLTemplateSource
import org.fusesource.scalate.Template
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.TemplateSource
import java.util.{Map => JUMap}

class SSPTemplateRenderer extends TemplateRenderer {

    private val cache : java.util.Map[String, Template] = new HashMap()
    private val sourceCache : JUMap[String, TemplateSource] = new HashMap()

    private val engine = new TemplateEngine

    def render(templateName: String, vars: JUMap[String, String], channel: Channel) {
        var template = cache.get(templateName)
        var templateSource : TemplateSource = null;
        if (template == null) {
            templateSource = new URLTemplateSource(this.getClass().getResource("/org/hibnet/elasticlogger/http/template.ssp"));
            template = engine.load(templateSource)
            cache.put(templateName, template)
            sourceCache.put(templateName, templateSource)
        } else {
            templateSource = sourceCache.get(templateName)
        }
        engine.layout(templateSource.uri, template, vars)
    }
}