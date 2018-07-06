package com.bossien.flowtreatmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 统计服务模块
 * @author gb
 */
@SpringBootApplication
@EnableEurekaClient
@ComponentScan
@EnableJpaRepositories
@EnableScheduling
public class FlowTreatmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowTreatmentServiceApplication.class, args);
	}
}
