package com.cloud.user.controller.merPak;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.config.RedisLock;
import com.cloud.user.listen.Response;
import com.cloud.user.listen.queue.ProductInventoryCacheRefreshRequest;
import com.cloud.user.listen.queue.ProductInventoryDBUpdateRequest;
import com.cloud.user.listen.queue.Request;
import com.cloud.user.service.merPak.ProductInventoryService;
import com.cloud.user.service.merPak.RequestAsyncProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class ProductInventoryController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private RequestAsyncProcessService requestAsyncProcessService;
    @Resource
    private ProductInventoryService productInventoryService;
    @Resource
    private RedisLock redisLock;

    /**
     * 更新商品库存
     */
    @RequestMapping("/updateProductInventory")
    @ResponseBody
    public Response updateProductInventory(@RequestBody MerInvetoryEntity productInventory) {
        // 为了简单起见，我们就不用log4j那种日志框架去打印日志了
        // 其实log4j也很简单，实际企业中都是用log4j去打印日志的，自己百度一下
        logger.info("===========日志===========: 接收到更新商品库存的请求，商品id=" + productInventory.getMerUid() + ", 商品库存数量=" + productInventory.getMerCount());

        Response response = null;

        try {
            Request request = new ProductInventoryDBUpdateRequest(
                    productInventory, productInventoryService,redisLock);
            requestAsyncProcessService.process(request);
            response = new Response(Response.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            response = new Response(Response.FAILURE);
        }

        return response;
    }

    /**
     * 获取商品库存
     */
    @RequestMapping("/getProductInventory")
    @ResponseBody
    public MerInvetoryEntity getProductInventory(long productId) {
        logger.info("===========日志===========: 接收到一个商品库存的读请求，商品id=" + productId);  ;

        MerInvetoryEntity productInventory = null;

        try {
            Request request = new ProductInventoryCacheRefreshRequest(productId, productInventoryService,true,redisLock);
            requestAsyncProcessService.process(request);

            // 将请求扔给service异步去处理以后，就需要while(true)一会儿，在这里hang住
            // 去尝试等待前面有商品库存更新的操作，同时缓存刷新的操作，将最新的数据刷新到缓存中
            long startTime = System.currentTimeMillis();
            long endTime = 0L;
            long waitTime = 0L;

            // 等待超过200ms没有从缓存中获取到结果
            while(true) {
                // 一般公司里面，面向用户的读请求控制在200ms就可以了
                if(waitTime > 200) {
                    break;
                }

                // 尝试去redis中读取一次商品库存的缓存数据
                productInventory = productInventoryService.getProductInventoryCache(productId);

                // 如果读取到了结果，那么就返回
                if(productInventory != null) {
                    logger.info("===========日志===========: 在200ms内读取到了redis中的库存缓存，商品id=" + productInventory.getMerUid() + ", 商品库存数量=" + productInventory.getMerCount());
                    return productInventory;
                }

                // 如果没有读取到结果，那么等待一段时间
                else {
                    Thread.sleep(20);
                    endTime = System.currentTimeMillis();
                    waitTime = endTime - startTime;
                }
            }

            // 直接尝试从数据库中读取数据
            productInventory = productInventoryService.findProductInventory(productId);
            if(productInventory != null) {
                // 将缓存刷新一下
                // 这个过程，实际上是一个读操作的过程，但是没有放在队列中串行去处理，还是有数据不一致的问题
                request = new ProductInventoryCacheRefreshRequest(
                        productId, productInventoryService, false,redisLock);
                requestAsyncProcessService.process(request);

                // 代码会运行到这里，只有三种情况：
                // 1、就是说，上一次也是读请求，数据刷入了redis，但是redis LRU算法给清理掉了，标志位还是false
                // 所以此时下一个读请求是从缓存中拿不到数据的，再放一个读Request进队列，让数据去刷新一下
                // 2、可能在200ms内，就是读请求在队列中一直积压着，没有等待到它执行（在实际生产环境中，基本是比较坑了）
                // 所以就直接查一次库，然后给队列里塞进去一个刷新缓存的请求
                // 3、数据库里本身就没有，缓存穿透，穿透redis，请求到达mysql库

                return productInventory;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new MerInvetoryEntity(Long.valueOf(productId), -1);
    }

}