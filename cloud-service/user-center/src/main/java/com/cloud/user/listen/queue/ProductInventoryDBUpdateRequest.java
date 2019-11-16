package com.cloud.user.listen.queue;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.config.RedisLock;
import com.cloud.user.service.ProductInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 比如说一个商品发生了交易，那么就要修改这个商品对应的库存
 *
 * 此时就会发送请求过来，要求修改库存，那么这个可能就是所谓的data update request，数据更新请求
 *
 * cache aside pattern
 *
 * （1）删除缓存
 * （2）更新数据库
 *
 *
 */
public class ProductInventoryDBUpdateRequest implements Request {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisLock redisLock;

    /**
     * 商品库存
     */
    private MerInvetoryEntity productInventory;
    /**
     * 商品库存Service
     */
    private ProductInventoryService productInventoryService;

    public ProductInventoryDBUpdateRequest(MerInvetoryEntity productInventory,
                                           ProductInventoryService productInventoryService) {
        this.productInventory = productInventory;
        this.productInventoryService = productInventoryService;
    }

    /**
     * 问题：更新缓存，我们得操作是先删除redis缓存，再更新数据库，一般是后台管理系统操作库存，不同于库存扣减这种并发性得操作。
     *   但是多个服务之间可能存在同时管理更新库存信息得情况，所以更新缓存操作可能加上分布式锁来避免这种情况
     */
    @Override
    public void process() {
        logger.info("===========日志===========: 数据库更新请求开始执行，商品id=" + productInventory.getMerUid() + ", 商品库存数量=" + productInventory.getMerCount());

        String uuid = UUID.randomUUID().toString();

        try{
            if (redisLock.lock(productInventory.getMerUid(), uuid)) {
                // 删除redis中的缓存
                productInventoryService.removeProductInventoryCache(productInventory);
                // 为了模拟演示先删除了redis中的缓存，然后还没更新数据库的时候，读请求过来了，这里可以人工sleep一下
                //		try {
                //			Thread.sleep(20000);
                //		} catch (InterruptedException e) {
                //			e.printStackTrace();
                //		}
                // 修改数据库中的库存
                productInventoryService.updateProductInventory(productInventory);
            }
        }catch (Exception e){}finally {
            redisLock.unlock(productInventory.getMerUid(), uuid);
        }
    }

    /**
     * 获取商品id
     */
    public long getProductId() {
        return productInventory.getMerUid();
    }


    @Override
    public boolean isForceRefresh() {
        return false;
    }

}
