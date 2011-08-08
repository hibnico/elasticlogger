package org.hibnet.elasticlogger.http
import org.fusesource.scalate.TemplateEngine

class Main {

    def main(args: Array[String]) {
        val engine = new TemplateEngine
        val output = engine.layout("/path/to/template.ssp", Map("name" -> "Hiram", "city" -> "Tampa"))
        
    }
}