package com.epam.e2etracingpoc.sourceadapter1;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.opentracing.TagProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {


    @Value("${camel.zipkin.tags.custom.target-system.name}")
    private String targetSystemName;

    @Value("${camel.zipkin.tags.custom.target-system.value}")
    private String targetSystemValue;


    @Override
    public void configure() throws Exception {

        from("kafka:product1source?brokers=kafka:9092&offsetRepository=#offsetRepo")
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .log(LoggingLevel.INFO, "Inbound message:\n ${headers}\n ${body}")
            .to("direct:sendToTarget")
//            .to("direct:filtering")
            .log(LoggingLevel.INFO, "Outbound message:\n ${headers}\n ${body}");

        from("direct:filtering").routeId("filtering")
                .choice()
                    .when(simple("${body[filterOut]}"))
                        .log("Message is filtered out")
                        .process(e -> e.getProperty("camel.client.customtags", Map.class).put("stage", "END"));

        from("direct:sendToTarget").routeId("sendToTarget")
            .process(new TagProcessor("stage", constant("END")))
            .choice()
                .when(simple("${body[fail2]}"))
                .process(new TagProcessor(targetSystemName, simple(targetSystemValue)))
                .process(new TagProcessor("stage", constant("ERROR")))
                .throwException(new RuntimeException("Some exception"))
            .otherwise()
                .process(new TagProcessor(targetSystemName, simple(targetSystemValue)))
                .process(new TagProcessor("stage", constant("END")))
            .end();
    }
}
