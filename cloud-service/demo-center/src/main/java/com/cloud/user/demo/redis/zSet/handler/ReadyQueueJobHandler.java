package com.cloud.user.demo.redis.zSet.handler;

import com.alibaba.fastjson.JSON;
import com.cloud.user.demo.redis.zSet.bean.DelayJob;
import com.cloud.user.demo.redis.zSet.bean.Job;
import com.cloud.user.demo.redis.zSet.constants.JobStatus;
import com.cloud.user.demo.redis.zSet.container.JobPool;
import com.cloud.user.demo.redis.zSet.container.ReadyQueue;
import com.cloud.user.redisConfig.RedisLock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Data
@AllArgsConstructor
public class ReadyQueueJobHandler implements Runnable {
    /**
     * 任务池
     */
    private JobPool jobPool;

    private final static String WEB = "web";

    private ReadyQueue readyQueue;

    RedisTemplate redisTemplate;


    @Override
    public void run() {
        log.info("定时任务开始消费");
        while (true) {
            try {
                // 移除并获得任务
                DelayJob delayJob = readyQueue.bLpopJob(WEB);
                //没有任务
                if (delayJob == null) {
                    continue;
                }
                Job job = jobPool.getJob(delayJob.getJodId());
                //延迟任务元数据不存在
                if (job == null) {
                    log.info("移除不存在任务:{}", JSON.toJSONString(delayJob));
                    continue;
                }
                //处理任务
                RedisLock redisLock = new RedisLock(redisTemplate,job.getId());
                try{
                    if (redisLock.lock()){
                        log.info("处理消费任务:{}", JSON.toJSONString(job));
                        // 超消费任务
                        processTtrJob(delayJob,job);
                    }
                }catch (Exception e){}finally {
                    redisLock.unlock();
                }
            } catch (Exception e) {
                log.error("扫描readyQueue出错：",e.getStackTrace());
            }finally {

            }
        }

    }

    private void processTtrJob(DelayJob delayJob, Job job) {
        try{
            job.setStatus(JobStatus.RESERVED);
            // 修改任务池状态RESERVED
            jobPool.addJob(job);

            //执行任务
            log.info("任务信息："+JSON.toJSONString(job));

            job.setStatus(JobStatus.DELETED);
            // 修改任务池状态DELETED
            jobPool.addJob(job);
            // 移除任务
            jobPool.removeDelayJob(delayJob.getJodId());
        }catch (Exception e){
            log.info("任务执行异常，重新放入消费队列："+JSON.toJSONString(job));
            job.setStatus(JobStatus.RESERVED);
            // 修改任务池状态RESERVED
            jobPool.addJob(job);
            readyQueue.pushJob(delayJob);
        }
    }
}
