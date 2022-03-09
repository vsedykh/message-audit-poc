package com.epam.e2etracingpoc.sourceadapter1;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.opentracing.TagProcessor;
import org.apache.camel.processor.ThreadsProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {

    @Value("${camel.zipkin.tags.custom.product-name.name}")
    private String productNameName;

    @Value("${camel.zipkin.tags.custom.product-name.value}")
    private String productNameValue;

    @Value("${camel.zipkin.tags.custom.source-system.name}")
    private String sourceSystemName;

    @Value("${camel.zipkin.tags.custom.source-system.value}")
    private String sourceSystemValue;

    @Value("${camel.zipkin.tags.custom.product-id.name}")
    private String productIdName;


    @Override
    public void configure() throws Exception {

        from("direct:processRequest").routeId("processRequest")
            .log(LoggingLevel.INFO, "Inbound message:\n ${headers}\n ${body}")
            .process(new TagProcessor(productNameName, constant(productNameValue)))
            .process(new TagProcessor(sourceSystemName, constant(sourceSystemValue)))
            .process(new TagProcessor(productIdName, header("X-Product-Id")))
            .process(new TagProcessor("stage", constant("START")))
            .marshal().json(JsonLibrary.Jackson)
            .to("kafka:product1source?brokers=kafka:9092")
            .log(LoggingLevel.INFO, "Outbound message:\n ${headers}\n ${body}");

            from("direct:test1").log("test1")
                .process(new TagProcessor("test", simple("test")));

    }
}
