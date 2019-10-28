package com.cloud.user.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloud.model.user.constants.UserCenterMq;


/**
 * Rabbitmq的exchange声明
 *
 * 这里声明一个topic类型的exchange，发消息时用。
 * 系统启动的时候就帮我们创建好TopicExchange
 */
@Configuration
public class RabbitmqConfig {

	@Bean
	public TopicExchange topicExchange() {
		return new TopicExchange(UserCenterMq.MQ_EXCHANGE_USER);
	}
}
