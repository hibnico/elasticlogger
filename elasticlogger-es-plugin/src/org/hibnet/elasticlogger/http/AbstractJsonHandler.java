package org.hibnet.elasticlogger.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public abstract class AbstractJsonHandler<I, O> extends AbstractHandler {

    protected HTTPParametersBeanMapper<I> inputMapper;

    protected ObjectMapper outputMapper = new ObjectMapper();

    public AbstractJsonHandler(Class<I> inputClass) {
        if (inputClass != null) {
            inputMapper = new HTTPParametersBeanMapper<I>(inputClass);
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        I input = null;
        if (inputMapper != null && request.getMethod().equals("POST")) {
            input = inputMapper.makeBean(request);
        }
        O output = handle(input, target, baseRequest, request, response);
        if (output != null) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            outputMapper.writeValue(response.getOutputStream(), output);
        }
    }

    abstract protected O handle(I input, String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}
