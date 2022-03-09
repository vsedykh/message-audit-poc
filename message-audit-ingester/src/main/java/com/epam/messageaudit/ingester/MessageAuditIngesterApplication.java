package com.epam.messageaudit.ingester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessageAuditIngesterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageAuditIngesterApplication.class, args);
    }

//    @Produce
//    @Bean("elasticsearch-rest")
//    ElasticsearchComponent elasticsearchComponent() {
//        ElasticsearchComponent elasticsearchComponent = new ElasticsearchComponent();
//        elasticsearchComponent.setHostAddresses("elasticsearch:9200");
//        return elasticsearchComponent;
//    }
}
