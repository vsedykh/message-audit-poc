package com.epam.e2etracingpoc.sourceadapter1;

import org.apache.camel.opentracing.starter.CamelOpenTracing;
//import org.apache.camel.zipkin.starter.CamelZipkin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@CamelZipkin
@CamelOpenTracing
public class Sourceadapter1Application {

	public static void main(String[] args) {
		SpringApplication.run(Sourceadapter1Application.class, args);
	}

}
