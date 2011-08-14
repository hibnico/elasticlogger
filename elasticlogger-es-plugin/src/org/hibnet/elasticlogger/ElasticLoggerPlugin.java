package org.hibnet.elasticlogger;

import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.plugins.AbstractPlugin;
import org.hibnet.elasticlogger.http.LoggerHttpServer;

public class ElasticLoggerPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "elasticlogger";
    }

    @Override
    public String description() {
        return "Web frontend of indexed java logs";
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(LoggerHttpServer.class);
        return services;
    }
}
