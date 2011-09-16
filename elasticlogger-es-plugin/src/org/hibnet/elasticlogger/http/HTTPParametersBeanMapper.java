package org.hibnet.elasticlogger.http;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public class HTTPParametersBeanMapper<T> {

    private Constructor<T> constructor;

    private Map<String, Field> fields = new HashMap<String, Field>();

    public HTTPParametersBeanMapper(Class<T> cl) {
        try {
            constructor = cl.getConstructor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Field field : cl.getDeclaredFields()) {
            fields.put(field.getName(), field);
        }
    }

    @SuppressWarnings("unchecked")
    public T makeBean(HttpServletRequest request) {
        return makeBean(request.getParameterMap());
    }

    public T makeBean(Map<String, String[]> parameters) {
        T bean;
        try {
            bean = constructor.newInstance();
            for (Entry<String, String[]> entry : parameters.entrySet()) {
                Field field = fields.get(entry.getKey());
                if (field != null) {
                    Object value = getValue(field.getGenericType(), entry.getValue());
                    field.set(bean, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bean;
    }

    private Object getValue(Type type, String[] value) {
        if (type.equals(String.class)) {
            return value[0];
        }
        if (type.equals(Long.class)) {
            return Long.parseLong(value[0]);
        }
        if (type.equals(Integer.class)) {
            return Integer.parseInt(value[0]);
        }
        if (type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value[0]);
        }
        throw new IllegalStateException("Unsupported type " + type);
    }

}
