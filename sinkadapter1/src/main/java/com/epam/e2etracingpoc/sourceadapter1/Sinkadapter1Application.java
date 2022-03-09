package com.epam.e2etracingpoc.sourceadapter1;

import java.io.File;
import org.apache.camel.component.kafka.consumer.support.KafkaConsumerResumeStrategy;
import org.apache.camel.component.kafka.consumer.support.OffsetKafkaConsumerResumeStrategy;
import org.apache.camel.impl.engine.FileStateRepository;
import org.apache.camel.opentracing.starter.CamelOpenTracing;
//import org.apache.camel.zipkin.starter.CamelZipkin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@CamelOpenTracing
public class Sinkadapter1Application {

	public static void main(String[] args) {
		SpringApplication.run(Sinkadapter1Application.class, args);
	}

	@Bean
	FileStateRepository offsetRepo() {
		return FileStateRepository.fileStateRepository(new File("/opt/repo.dat"));
	}

	@Bean
	KafkaConsumerResumeStrategy resumeStrategy(){
		return new OffsetKafkaConsumerResumeStrategy(offsetRepo());
	}
}
