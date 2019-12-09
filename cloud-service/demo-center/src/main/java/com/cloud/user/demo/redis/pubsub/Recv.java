package com.cloud.user.demo.redis.pubsub;

import org.springframework.stereotype.Component;

@Component
public class Recv {
    /**
     * //具体操作方法 名称随意
     * @param msg
     */
    public void recvMsg(String msg) {
        System.out.println("收到消息：" + msg);
    }
}