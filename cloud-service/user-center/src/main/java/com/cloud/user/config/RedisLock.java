package com.cloud.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 *
 * 所谓 SETNX，是「SET if Not eXists」的缩写，也就是只有不存在的时候才设置，可以利用它来实现锁的效果
 *
 * 缓存过期时，通过 SetNX  获取锁，如果成功了，那么更新缓存，然后删除锁。看上去逻辑非常简单，可惜有问题：如果请求执行因为某些原因意外退出了，
 * 导致创建了锁但是没有删除锁，那么这个锁将一直存在，以至于以后缓存再也得不到更新。于是乎我们需要给锁加一个过期时间以防不测
 *
 * RedisLock的正确姿势
 * 加锁：
 * 通过setnx 向特定的key写入一个随机数，并设置失效时间，写入成功即加锁成功
 * 注意点：
 *  必须给锁设置一个失效时间            ----->    避免死锁
 *  加锁时，每个节点产生一个随机字符串    ----->    避免锁误删
 *  写入随机数与设置失效时间必须是同时    ----->    保证加锁的原子性
 *  使用：
 *      SET key value NX PX 3000
 *
 *
 * 解锁：
 *  匹配随机数，删除redis上的特定的key数据，
 *  要保证获取数据，判断一致以及删除数据三个操作是原子性
 *  执行如下lua脚本：
 *      if redis.call('get', KEYS[1]) == ARGV[1] then
 *          return redis.call('del', KEYS[1])
 *      else
 *          return 0
 *      end
 *
 *
 *
 *      从 Redis 2.6.12 版本开始， SET 命令的行为可以通过一系列参数来修改：
 *
 * EX seconds ： 将键的过期时间设置为 seconds 秒。 执行 SET key value EX seconds 的效果等同于执行 SETEX key seconds value 。
 * PX milliseconds ： 将键的过期时间设置为 milliseconds 毫秒。 执行 SET key value PX milliseconds 的效果等同于执行 PSETEX key milliseconds value 。
 * NX ： 只在键不存在时， 才对键进行设置操作。 执行 SET key value NX 的效果等同于执行 SETNX key value 。
 * XX ： 只在键已经存在时， 才对键进行设置操作。
 *
 */
@Service
public class RedisLock {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private String lock_key = "redis_lock"; //锁键

    protected long internalLockLeaseTime = 30000;//锁过期时间

    private long timeout = 999999; //获取锁的超时时间

    //SET命令的参数
//    SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    @Autowired
    JedisPool jedisPool;

    /**
     * 加锁
     * @param id
     * @return
     */
    public boolean lock(Long id,String uuid){
        Jedis jedis = jedisPool.getResource();
        Long start = System.currentTimeMillis();
        String lockStr = lock_key + "_" + id;
        try{
            while(true){
//                logger.info("线程尝试获取锁:"+Thread.currentThread().getName());
                //SET命令返回OK ，则证明获取锁成功
                String lock = jedis.set(lockStr, uuid, "nx","px",internalLockLeaseTime);
                if("OK".equals(lock)){
                    logger.info("线程获取锁成功:"+Thread.currentThread().getName());
                    return true;
                }
                //否则循环等待，在timeout时间内仍未获取到锁，则获取失败
                long l = System.currentTimeMillis() - start;
                if (l>=timeout) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }finally {
            jedis.close();
        }
    }


    /**
     * 解锁
     * @param
     * @return
     */
    public boolean unlock(Long id,String uuid){
        String lockStr = lock_key + "_" + id;
        Jedis jedis = jedisPool.getResource();

        StringBuffer scricpt = new StringBuffer();
        scricpt.append("if redis.call('get',KEYS[1]) == ARGV[1] then");
        scricpt.append("   return redis.call('del',KEYS[1]) ");
        scricpt.append(" else ");
        scricpt.append(" return 0 ");
        scricpt.append(" end ");
        try {
            Object result = jedis.eval(scricpt.toString(), Collections.singletonList(lockStr),
                    Collections.singletonList(uuid));
            if("1".equals(result.toString())){
                logger.info("线程释放锁成功:"+Thread.currentThread().getName());
                return true;
            }
            logger.info("线程未找到锁:"+Thread.currentThread().getName());
            return false;
        }finally {
            jedis.close();
        }
    }


}
