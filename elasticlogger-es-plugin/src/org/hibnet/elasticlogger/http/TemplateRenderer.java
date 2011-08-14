package org.hibnet.elasticlogger.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;

public class TemplateRenderer {

    private VelocityEngine velocityEngine = new VelocityEngine();

    public static final class TemplateLoader extends ClasspathResourceLoader {
        @Override
        public InputStream getResourceStream(String name) throws ResourceNotFoundException {
            return super.getResourceStream("/org/hibnet/elasticlogger/http/resources/" + name);
        }
    }

    public TemplateRenderer() {
        Properties config = new Properties();
        config.setProperty("resource.loader", "classpath");
        config.setProperty("classpath.resource.loader.class", TemplateLoader.class.getName());
        velocityEngine.init(config);
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
