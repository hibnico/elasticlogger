package org.hibnet.elasticlogger.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.eclipse.jetty.server.Request;

public class TemplateRenderer {

    private VelocityEngine velocityEngine = new VelocityEngine();

    public static final class TemplateLoader extends ClasspathResourceLoader {
        @Override
        public InputStream getResourceStream(String name) throws ResourceNotFoundException {
            return super.getResourceStream("/org/hibnet/elasticlogger/http/" + name);
        }
    }

    public TemplateRenderer() {
        Properties config = new Properties();
        config.setProperty("resource.loader", "classpath");
        config.setProperty("classpath.resource.loader.class", TemplateLoader.class.getName());
        velocityEngine.init(config);
    }

    public void render(Request baseRequest, HttpServletResponse response, String templateName, Map<String, Object> bindings)
            throws ResourceNotFoundException, ParseErrorException, MethodInvocationException, IOException {
        baseRequest.setHandled(true);
        bindings.put("content_template", templateName);
        velocityEngine.mergeTemplate("layout.html", "UTF-8", new VelocityContext(bindings), response.getWriter());
    }
}
