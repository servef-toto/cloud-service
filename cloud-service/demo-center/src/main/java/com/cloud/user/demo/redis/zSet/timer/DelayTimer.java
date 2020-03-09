package com.cloud.user.demo.redis.zSet.timer;

import com.cloud.user.demo.redis.zSet.container.DelayBucket;
import com.cloud.user.demo.redis.zSet.container.JobPool;
import com.cloud.user.demo.redis.zSet.container.ReadyQueue;
import com.cloud.user.demo.redis.zSet.handler.DelayJobHandler;
import com.cloud.user.demo.redis.zSet.handler.ReadyQueueJobHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

//@Component
public class DelayTimer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private DelayBucket delayBucket;
    @Autowired
    private JobPool jobPool;
    @Autowired
    private ReadyQueue readyQueue;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @Value("${thread.size}")
    private int length;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ExecutorService executorService = new ThreadPoolExecutor(
                length+1,
                length+1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        for (int i = 0; i < length; i++) {
            executorService.execute(
                    new DelayJobHandler(
                            delayBucket,
                            jobPool,
                            readyQueue,
                            i,
                            redisTemplate));
        }

        //处理延迟消息线程
        for (int i = 0; i < 1; i++) {
            executorService.execute(
                    new ReadyQueueJobHandler(
                            jobPool,
                            readyQueue,
                            redisTemplate));
        }

    }
}