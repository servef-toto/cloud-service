package com.cloud.user.demo.redis.pubsub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter recvAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(recvAdapter, new PatternTopic("channel1"));//将订阅者1与channel1频道绑定
        return container;
    }
    @Bean
    MessageListenerAdapter recvAdapter(Recv receiver){ //与channel1绑定的适配器
        return new MessageListenerAdapter(receiver, "recvMsg");/*收到消息时执行Recv类中的
                                                                 recvMsg方法*/
    }
}