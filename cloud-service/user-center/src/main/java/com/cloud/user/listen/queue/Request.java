package com.cloud.user.listen.queue;

/**
 * 请求接口
 */
public interface Request {
    void process();

    long getProductId();
    boolean isForceRefresh();
}
