package com.cloud.user.demo.redis.list;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class RedisMq {

    private static String key = "redis-test";

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 发送消息
     * @param message
     */
    public void push(String message){
        redisTemplate.opsForList().leftPush(key,message);
    }

    /**
     * 获取消息,可以对消息进行监听，没有超过监听事件，则返回消息为null
     * rightPop：1.key,2.超时时间，3.超时时间类型
     * @return
     */
    public String pop(){
        return (String) redisTemplate.opsForList().rightPop(key,60, TimeUnit.SECONDS);
    }
}