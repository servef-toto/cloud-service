package com.cloud.user.demo.redis.zSet.container;

import com.alibaba.fastjson.JSON;
import com.cloud.user.demo.redis.zSet.bean.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 任务池hash:job任务池，为普通的K/V结构，提供基础的操作
 **/
@Component
@Slf4j
public class JobPool {

    @Autowired
    private RedisTemplate redisTemplate;

    private String NAME = "job.pool";

    private BoundHashOperations getPool () {
        BoundHashOperations ops = redisTemplate.boundHashOps(NAME);
        return ops;
    }

    /**
     * 添加任务
     * @param job
     */
    public void addJob (Job job) {
        log.info("任务池添加任务：{}", JSON.toJSONString(job));
        getPool().put(job.getId(),job);
    }

    /**
     * 获得任务
     * @param jobId
     * @return
     */
    public Job getJob(String jobId) {
        Object o = getPool().get(jobId);
        if (o instanceof Job) {
            return (Job) o;
        }
        return null;
    }

    /**
     * 移除任务
     * @param jobId
     */
    public void removeDelayJob (String jobId) {
        log.info("任务池移除任务：{}",jobId);
        // 移除任务
        getPool().delete(jobId);
    }

}
