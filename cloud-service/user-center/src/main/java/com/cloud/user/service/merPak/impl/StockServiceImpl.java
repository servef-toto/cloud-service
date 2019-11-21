package com.cloud.user.service.merPak.impl;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.config.RedisLock;
import com.cloud.user.dao.StockDao;
import com.cloud.user.listen.queue.RequestQueue;
import com.cloud.user.service.merPak.IStockCallback;
import com.cloud.user.service.merPak.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 缓存中的数据为实时库存，数据库库存为实际库存，要保证两边一致性
 * <p>
 * 方案1：考虑在每次库存发生扣减后，异步发送mq消息去进行更改数据库的数据操作，会发生问题：
 * 每次库存发生变化，首先是缓存更新，更新完之后异步进行数据库的更新。
 * 问题出在如果还没有缓存的情况下，就需要先去数据库拿缓存，此时就开始拿到锁，如果此时缓存刚好因为某些原因失效了没有了（比如到了过期时间没有了），
 * 而之前该库存发送改变而发送的mq还未消费完同步到数据库做实际库存，此时去数据库拿数据进行初始化缓存的话，可能就会造成数据不一致了数据丢失。
 */
@Service
public class StockServiceImpl implements StockService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StockDao stockDao;

    @Autowired
    private RedisLock redisLock;

    /**
     * 库存还未初始化
     */
    public static final long UNINITIALIZED_STOCK = -3L;


    public static final String KRY_PRE = "redis_key:stock:";

    /**
     * @param key
     * @return -3:库存未初始化
     */
    @Override
    public int getStock(String key) {
        String rediskey = KRY_PRE + key;
        Integer stock = (Integer) redisTemplate.opsForValue().get(rediskey);
        return stock == null ? -3 : stock;
    }

    /**
     * 增加库存
     *
     * @param key    库存key
     * @param expire 过期时间（秒）
     * @param num    库存数量
     * @return
     */
    @Override
    @Transactional
    public long addStock(long key, long expire, int num) {
        return this.addStockNum(key, expire, num);
    }

    private long addStockNum(Long key, Long expire, int num) {
        String uuid = UUID.randomUUID().toString();
        String rediskey = KRY_PRE + key;
        // 判断是否存在库存,有库存则为true，否则为false
        Boolean hasKey = redisTemplate.hasKey(rediskey);
        if (hasKey) {
            // 有库存的情况进行自增操作 redisTemplate的increment自增操作
            return redisTemplate.opsForValue().increment(rediskey, num);
        } else {
            // 如果没有初始化库存操作
            Assert.notNull(expire, "初始化库存失败，库存过期时间不能为null");
            try {
                // 初始化操作
                if (redisLock.lock(key, uuid)) {
                    // 获取到锁后再次判断一下是否有key,二次验证
                    hasKey = redisTemplate.hasKey(rediskey);
                    if (!hasKey) {
                        // 获取数据库数据
                        MerInvetoryEntity entity = stockDao.findById(key);
                        Integer count = num + (entity == null ? 0 : entity.getMerCount());
                        // 初始化库存
                        redisTemplate.opsForValue().set(rediskey, count, expire, TimeUnit.SECONDS);
                        //修改标识位为false，表示查看库存的时候直接获取缓存就可以
                        updateQueueFlagFalse(key);
                        num = count;
                    } else {
                        // 有库存的情况进行自增操作 redisTemplate的increment自增操作
                        return redisTemplate.opsForValue().increment(rediskey, num);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                redisLock.unlock(key, uuid);
            }
        }
        return num;
    }

    @Override
    @Transactional
    public Object addBatchStock(List<Object> commodityIds, long expire, int num) throws InterruptedException {
        Assert.isTrue(commodityIds != null && !commodityIds.isEmpty(), "新增库存失败，商品不能为null");

        CountDownLatch countDownLatch = new CountDownLatch(commodityIds.size());
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(commodityIds.size(),
                commodityIds.size(),
                1,
                TimeUnit.MINUTES,new ArrayBlockingQueue<>(commodityIds.size()));
        for (int i=0;i<commodityIds.size();i++){
            Map<String,String> m = (Map<String, String>) commodityIds.get(i);
            threadPoolExecutor.execute(() -> {
                this.addStockNum(Long.valueOf(m.get("commodityId")),expire,num);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        return "操作成功:"+countDownLatch.getCount();
    }

    /**
     * 执行扣库存的脚本
     */
    public static final String STOCK_LUA;

    static {
        /**
         *
         * @desc 扣减库存Lua脚本
         * 库存（stock）-1：表示不限库存
         * 库存（stock）0：表示没有库存
         * 库存（stock）大于0：表示剩余库存
         *
         * @params 库存key
         * @return
         *      -3:库存未初始化
         * 		-2:库存不足
         * 		-1:不限库存
         * 		大于等于0:剩余库存（扣减之后剩余的库存）,直接返回-1
         */
        StringBuilder sb = new StringBuilder();
        // exists 判断是否存在KEY，如果存在返回1，不存在返回0
        sb.append("if (redis.call('exists', KEYS[1]) == 1) then");
        // get 获取KEY的缓存值，tonumber 将redis数据转成 lua 的整形
        sb.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        sb.append("    local num = tonumber(ARGV[1]);");
        // 如果拿到的缓存数等于 -1，代表改商品库存是无限的，直接返回1
        sb.append("    if (stock == -1) then");
        sb.append("        return -1;");
        sb.append("    end;");
        // incrby 特性进行库存的扣减
        sb.append("    if (stock >= num) then");
        sb.append("        return redis.call('incrby', KEYS[1], 0-num);");
        sb.append("    end;");
        sb.append("    return -2;");
        sb.append("end;");
        sb.append("return -3;");
        STOCK_LUA = sb.toString();
    }


    /**
     * @param id           库存key
     * @param expire        库存有效时间,单位秒
     * @param num           扣减数量
     * @param stockCallback 初始化库存回调函数
     * @return -2:库存不足; -1:不限库存; 大于等于0:扣减库存之后的剩余库存
     */
    public long stock(long id, long expire, int num, IStockCallback stockCallback) {
        String uuid = UUID.randomUUID().toString();
        String key = KRY_PRE + id;
        long stock = stock(key, num);
        // 初始化库存
        if (stock == UNINITIALIZED_STOCK) {
            try {
                // 获取锁
                if (redisLock.lock(id, uuid)) {
                    // 双重验证，避免并发时重复回源到数据库
                    stock = stock(key, num);
                    if (stock == UNINITIALIZED_STOCK) {
                        // 获取初始化库存
                        int initStock = stockCallback.getStock();
                        // 将库存设置到redis
                        redisTemplate.opsForValue().set(String.valueOf(key), initStock, expire, TimeUnit.SECONDS);
                        // 调一次扣库存的操作
                        stock = stock(key, num);

                        //修改标识位为false，表示查看库存的时候直接获取缓存就可以
                        updateQueueFlagFalse(id);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                redisLock.unlock(id, uuid);
            }

        }
        return stock;
    }

    /**
     * 扣库存
     *
     * @param key 库存key
     * @param num 扣减库存数量
     * @return 扣减之后剩余的库存【-3:库存未初始化; -2:库存不足; -1:不限库存; 大于等于0:扣减库存之后的剩余库存】
     */
    private Long stock(String key, int num) {
        // 脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        keys.add(String.valueOf(key));
        // 脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(Integer.toString(num));

        long result = redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式和单机模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
                // 集群模式
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(STOCK_LUA, keys, args);
                }

                // 单机模式
                else if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).eval(STOCK_LUA, keys, args);
                }
                return UNINITIALIZED_STOCK;
            }
        });
        return result;
    }

    @Override
    public Object batchStock(Map<String,List<Map<String,Integer>>> map, long expire) throws InterruptedException {
        Assert.isTrue(map != null && !map.isEmpty(), "扣减库存失败，商品不能为null");

        List<Map<String,Integer>> list = map.get("commodityIds");
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(list.size(),
                list.size(),
                1,
                TimeUnit.MINUTES,new ArrayBlockingQueue<>(list.size()));
        for (int i=0;i<list.size();i++){
            Map<String,Integer> m = list.get(i);
            long id = Long.valueOf(m.get("commodityId"));
            int num = m.get("num");
            threadPoolExecutor.execute(() -> {
                this.stock(id,expire,num,() -> {
                    MerInvetoryEntity entity = stockDao.findById(id);
                    return entity!=null?entity.getMerCount():0;
                });
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        return "操作成功:"+countDownLatch.getCount();
    }














    int count = 0;

    @Override
    public String locktest() throws InterruptedException {
        count = 0;
        long l = 7;
        int clientcount = 100;
        CountDownLatch countDownLatch = new CountDownLatch(clientcount);

        ExecutorService executorService = Executors.newFixedThreadPool(clientcount);
        long start = System.currentTimeMillis();
        for (int i = 0; i < clientcount; i++) {
            executorService.execute(() -> {

                //通过Snowflake算法获取唯一的ID字符串
                String id = UUID.randomUUID().toString();
                try {
                    redisLock.lock(l, id);
                    count++;
                } finally {
                    redisLock.unlock(l, id);
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        logger.info("执行线程数:{},总耗时:{},count数为:{}", clientcount, end - start, count);
        return "Hello";
    }




    private void updateQueueFlagFalse(Long key) {
        //获取所有内存请求队列
        RequestQueue requestQueue = RequestQueue.getInstance();
        //获取所有得标识位
        Map<Long, Boolean> flagMap = requestQueue.getFlagMap();
        //修改标识位置
        flagMap.put(key, false);
    }

}
