package com.epam.messageaudit.ingester;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("kafka:message-audit?brokers=kafka:9092").routeId("message-audit-el-ingester")
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .setHeader("indexId", simple("${body[traceID]}"))
            .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http.HttpMethods.PUT))
            .setHeader(Exchange.HTTP_PATH, simple("message-audit-2022-02-28/_doc/${header.indexId}"))
            .marshal().json(JsonLibrary.Jackson)
            .to("http:elasticsearch:9200")
            .log("${headers} ${body}");
    }
}
