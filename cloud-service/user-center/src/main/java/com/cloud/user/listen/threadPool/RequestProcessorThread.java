package com.cloud.user.listen.threadPool;

import com.cloud.user.listen.queue.ProductInventoryCacheRefreshRequest;
import com.cloud.user.listen.queue.ProductInventoryDBUpdateRequest;
import com.cloud.user.listen.queue.Request;
import com.cloud.user.listen.queue.RequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

public class RequestProcessorThread implements Callable {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 自己监控的内存队列
     */
    private ArrayBlockingQueue <Request> queue;

    public RequestProcessorThread(ArrayBlockingQueue<Request> queue) {
        this.queue = queue;
    }


    @Override
    public Object call() throws Exception {
        try{
            while (true){
                // ArrayBlockingQueue 得 take和put相互对应，如果队列满了，或者是空的，那么都会在执行操作的时候，阻塞住，一直到能够取到数据
                Request request = queue.take();

                // 是否强制刷新
                if (request.isForceRefresh()){
                    //获取所有内存请求队列
                    RequestQueue requestQueue = RequestQueue.getInstance();
                    //获取所有得标识位
                    Map<Long, Boolean> flagMap = requestQueue.getFlagMap();

                    if(request instanceof ProductInventoryDBUpdateRequest) {
                        //问题：更新数据，我们得操作是先删除redis缓存，再更新数据库，一般是后台管理系统操作库存，不同于库存扣减这种并发性得操作。
                        // 但是多个服务之间可能存在同时管理更新库存信息得情况，所以更新缓存操作可能加上分布式锁来避免这种情况，再请求里面做


                        // 如果是一个更新数据库的请求，那么就将那个productId对应的标识设置为true
                        flagMap.put(request.getProductId(), true);
                    } else if(request instanceof ProductInventoryCacheRefreshRequest) {
                        //问题：刷新数据，我们得操作是先从数据库读取真正得库存，然后更新到redis缓存。一般库存查询经常用到属于高并发得操作。
                        // 多个服务之前同一时间可能存在对同一个商品进行多次请求。所以可以把用来判断是否已经有缓存得标识符设置在redis中，或者可以之间用来判断该商品库存是否有缓存，有缓存就可以不进行更新缓存操作，并且当前请求直接返回缓存数据

                        Boolean flag = flagMap.get(request.getProductId());

                        // 如果flag是null
                        if(flag == null) {
                            flagMap.put(request.getProductId(), false);
                        }

                        // 如果是缓存刷新的请求，那么就判断，如果标识不为空，而且是true，就说明之前有一个这个商品的数据库更新请求
                        if(flag != null && flag) {
                            flagMap.put(request.getProductId(), false);
                        }

                        // 如果是缓存刷新的请求，而且发现标识不为空，但是标识是false
                        // 说明前面已经有一个数据库更新请求+一个缓存刷新请求了
                        if(flag != null && !flag) {
                            // 对于这种读请求，直接就过滤掉，不要放到后面的内存队列里面去执行了
                            logger.info("===========日志===========: 该商品已经存在最新的库存，无需放到内存队列里面去执行，商品id=" + request.getProductId());
                            return true;
                        }
                    }

                }
                logger.info("===========日志===========: 工作线程处理请求，商品id=" + request.getProductId());
                // 执行这个request操作
                request.process();


                // 假如说，执行完了一个读请求之后，假设数据已经刷新到redis中了
                // 但是后面可能redis中的数据会因为内存满了，被自动清理掉
                // 如果说数据从redis中被自动清理掉了以后
                // 然后后面又来一个读请求，此时如果进来，发现标志位是false，就不会去执行这个刷新的操作了
                // 所以在执行完这个读请求之后，实际上这个标志位是停留在false的
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }
}
