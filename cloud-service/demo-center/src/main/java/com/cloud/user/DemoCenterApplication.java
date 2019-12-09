package com.cloud.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * 用户中心
 *
 *
 */
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class DemoCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoCenterApplication.class, args);
	}

}