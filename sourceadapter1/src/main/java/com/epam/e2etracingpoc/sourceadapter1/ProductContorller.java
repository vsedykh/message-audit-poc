package com.epam.e2etracingpoc.sourceadapter1;

import java.util.Map;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductContorller {

    @EndpointInject("direct:processRequest")
    protected ProducerTemplate producerTemplate;

    @PostMapping("/product")
    void newProduct(@RequestBody Map<String, Object> product, @RequestHeader("X-Product-Id") String productId) {
        System.out.println(product);
        System.out.println(productId);
        producerTemplate.sendBodyAndHeader(product, "X-Product-Id", productId);
    }
}
