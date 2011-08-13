package org.hibnet.elasticlogger.http
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.support.URLTemplateSource
import java.net.URL
import org.fusesource.scalate.Binding

object Main {

    def main(args: Array[String]) {
        var t = System.currentTimeMillis();

        val engine = new TemplateEngine
        val templateSource = new URLTemplateSource(this.getClass().getResource("/org/hibnet/elasticlogger/http/template.ssp"));

        println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis()

        val template = engine.load(templateSource)

        println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis()

        val output = engine.layout(templateSource.uri, template, Map("name" -> "Hiram", "city" -> "Tampa"))

        println(System.currentTimeMillis() - t);
        println(output);
    }
}