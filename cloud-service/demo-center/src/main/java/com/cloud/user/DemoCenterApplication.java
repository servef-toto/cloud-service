package com.cloud.user;

import com.cloud.user.demo.redis.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * 用户中心
 */
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class DemoCenterApplication {
	static Logger logger = LoggerFactory.getLogger(DemoCenterApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DemoCenterApplication.class, args);

		logger.info("启动2");
		logger.warn("启动2");
		logger.error("启动2");
		logger.debug("启动2");
	}

}