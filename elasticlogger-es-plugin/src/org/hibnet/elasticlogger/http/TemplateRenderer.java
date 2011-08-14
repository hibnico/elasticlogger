package org.hibnet.elasticlogger.http;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;

public class TemplateRenderer {

    private VelocityEngine velocityEngine = new VelocityEngine();

    public TemplateRenderer() {
        velocityEngine.init();
    }

    public ChannelBuffer render(String templateName, Map<String, Object> bindings) {
        Template template = velocityEngine.getTemplate(templateName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        try {
            template.merge(new VelocityContext(bindings), writer);
        } finally {
            writer.close();
        }

        return ChannelBuffers.copiedBuffer(baos.toByteArray());
    }
}
