package com.cloud.user.config;

import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 开启session共享
 *  spring boot提供了session用redis解决共享的方法
 *  springboot 需要引入 redis、spring-session-data-redis 相关jar包
 *  配置了redis后，EnableRedisHttpSession注解开启 session 共享
 */

/**
 * maxInactiveIntervalInSeconds 默认是1800秒过期，这里测试修改为60秒
 * 打开@EnableRedisHttpSession源码，发现maxInactiveIntervalInSeconds 、
 * session的过期时间默认是1800秒即30分钟，如果需要修改，注解时进行修改即可。
 * 如果想对redisSession做一些特殊处理。看@EnableRedisHttpSession源码，头部的注释，也给出了一些方案。
 */
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 60)
public class SessionConfig {
}
