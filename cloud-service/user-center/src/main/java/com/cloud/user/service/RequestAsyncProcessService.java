package com.cloud.user.service;

import com.cloud.user.listen.queue.Request;

public interface RequestAsyncProcessService {

    void process(Request request);
}
