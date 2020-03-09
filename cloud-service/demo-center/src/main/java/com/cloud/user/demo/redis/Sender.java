package com.cloud.user.demo.redis;

import com.cloud.user.demo.redis.list.RedisMq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
public class Sender{
    Logger logger = LoggerFactory.getLogger(Sender.class);

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    /*当这个方法被调用时，将会发布消息到channel1 然后订阅者执行对应的方法
    这里就会在控制台打印hello world*/
    @RequestMapping("/sender")
    public String send(){
        redisTemplate.convertAndSend("channel1","hello world");
        return "SUCCESS";
    }




    @Autowired
    RedisMq redisMq;

    //在redis中存储消息
    @GetMapping("/push")
    public Object pushMsg(@RequestParam("msg")String msg){
        redisMq.push(msg);
        return "SUCCESS";
    }

    //从redis中获取消息
    @GetMapping("/pop")
    public Object popMsg(){
        return redisMq.pop();
    }



    @GetMapping("/traceuid")
    public Object traceuid() throws InterruptedException {
        logger.info("调用方法traceuid");
        Map<String, String> map = MDC.getCopyOfContextMap();
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10,
//                10,
//                10,
//                TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(10));
//        for (int i = 0; i < 10; i++) {
//            int finalI = i;
//            threadPoolExecutor.execute(() -> {
//                logger.info("调用线程{}", finalI);
//            });
//        }
//
//        this.wait(10);
//        threadPoolExecutor.shutdown();
        logger.info("方法返回traceuid");
        return map;
    }
}