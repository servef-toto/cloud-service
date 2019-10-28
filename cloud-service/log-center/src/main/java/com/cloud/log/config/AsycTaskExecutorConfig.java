package com.cloud.log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池配置、启用异步
 */
@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsycTaskExecutorConfig {
	@Bean
	public TaskExecutor asynTaskExcutor(){
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		// 配置核心线程数
		taskExecutor.setCorePoolSize(50);
		// 配置最大线程数
		taskExecutor.setMaxPoolSize(100);
		return taskExecutor;
	}
}
