package com.cloud.user.redisConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class JedisConfig extends CachingConfigurerSupport {
    private Logger logger = LoggerFactory.getLogger(JedisConfig.class);

    /**
     * SpringSession  需要注意的就是redis需要2.8以上版本，然后开启事件通知，在redis配置文件里面加上
     * notify-keyspace-events Ex
     * Keyspace notifications功能默认是关闭的（默认地，Keyspace 时间通知功能是禁用的，因为它或多或少会使用一些CPU的资源）。
     * 或是使用如下命令：
     * redis-cli config set notify-keyspace-events Egx
     * 如果你的Redis不是你自己维护的，比如你是使用阿里云的Redis数据库，你不能够更改它的配置，那么可以使用如下方法：在applicationContext.xml中配置
     * <util:constant static-field="org.springframework.session.data.redis.config.ConfigureRedisAction.NO_OP"/>
     * @return
     */

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.port}")
    private int port;

//    @Value("${spring.redis.timeout}")
//    private int timeout;

    @Value("${spring.redis.jedis.pool.max-active}")
    private int maxActive;

    @Value("${spring.redis.jedis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.redis.jedis.pool.min-idle}")
    private int minIdle;

    @Value("${spring.redis.jedis.pool.max-wait}")
    private long maxWaitMillis;



    @Bean
    public JedisPool redisPoolFactory(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle); //最大能够保持idle的数量，控制一个pool最多有多少个状态为idle的jedis实例
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);  // 连接池最大等待时间 100s，如果在规定时间没有获取到则会报错,（使用负值表示没有限制）
        jedisPoolConfig.setMaxTotal(maxIdle); //在获取当时通过pool能够获取到的最大的连接的jedis个数（已经被客户端 连接上或者正在闲置等待客户端连接），默认8
        jedisPoolConfig.setMinIdle(minIdle); //最小的闲置连接数，仅仅在此值为正数且timeBetweenEvictionRunsMillis值大于0时有效
        JedisPool jedisPool = new JedisPool(jedisPoolConfig,host,port,1000,password);

        logger.info("JedisPool注入成功！");
        logger.info("redis地址：" + host + ":" + port);
        return  jedisPool;
    }

}