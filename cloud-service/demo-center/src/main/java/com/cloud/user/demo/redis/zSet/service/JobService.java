package com.cloud.user.demo.redis.zSet.service;

import com.cloud.user.demo.redis.zSet.bean.DelayJob;
import com.cloud.user.demo.redis.zSet.bean.Job;
import com.cloud.user.demo.redis.zSet.constants.JobStatus;
import com.cloud.user.demo.redis.zSet.container.DelayBucket;
import com.cloud.user.demo.redis.zSet.container.JobPool;
import com.cloud.user.demo.redis.zSet.container.ReadyQueue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobService {

    @Autowired
    private DelayBucket delayBucket;

    @Autowired
    private ReadyQueue readyQueue;

    @Autowired
    private JobPool jobPool;


    public DelayJob addDefJob(Job job) {
        job.setStatus(JobStatus.DELAY);
        jobPool.addJob(job);
        DelayJob delayJob = new DelayJob(job);
        delayBucket.addDelayJob(delayJob);
        return delayJob;
    }

    /**
     * 获取
     * @return
     */
    public Job getProcessJob(String topic) {
        // 拿到任务
        DelayJob delayJob = readyQueue.popJob(topic);
        if (delayJob == null || StringUtils.isEmpty(delayJob.getJodId())) {
            return new Job();
        }
        Job job = jobPool.getJob(delayJob.getJodId());
        // 元数据已经删除，则取下一个
        if (job == null) {
            job = getProcessJob(topic);
        } else {
            job.setStatus(JobStatus.RESERVED);
            delayJob.setDelayDate(System.currentTimeMillis() + job.getTtrTime());

            jobPool.addJob(job);
            delayBucket.addDelayJob(delayJob);
        }
        return job;
    }

    /**
     * 完成一个执行的任务
     * @param jobId
     * @return
     */
    public void finishJob(String jobId) {
        jobPool.removeDelayJob(jobId);
    }

    /**
     * 完成一个执行的任务
     * @param jobId
     * @return
     */
    public void deleteJob(String jobId) {
        jobPool.removeDelayJob(jobId);
    }

}