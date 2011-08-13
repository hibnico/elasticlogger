package org.hibnet.elasticlogger;

import org.elasticsearch.plugins.AbstractPlugin;

public class ElasticLoggerPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "elasticlogger";
    }

    @Override
    public String description() {
        return "Web frontend of indexed java logs";
    }

}
