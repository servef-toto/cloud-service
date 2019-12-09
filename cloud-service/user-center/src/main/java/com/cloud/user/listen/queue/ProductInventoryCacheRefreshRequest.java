package com.cloud.user.listen.queue;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.config.RedisLock;
import com.cloud.user.service.merPak.ProductInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 重新加载商品库存的缓存
 * @author Administrator
 *
 */
public class ProductInventoryCacheRefreshRequest implements Request {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private RedisLock redisLock;
    /**
     * 商品id
     */
    private long productId;
    /**
     * 商品库存Service
     */
    private ProductInventoryService productInventoryService;
    /**
     * 是否强制刷新缓存
     */
    private boolean forceRefresh;

    public ProductInventoryCacheRefreshRequest(long productId,
                                               ProductInventoryService productInventoryService,
                                               boolean forceRefresh,
                                               RedisLock redisLock) {
        this.productId = productId;
        this.productInventoryService = productInventoryService;
        this.forceRefresh = forceRefresh;
        this.redisLock = redisLock;
    }

    @Override
    public void process() {
        String uuid = UUID.randomUUID().toString();
        try{
            //获取锁，如果拿不到锁立即返回，说明已经有一个操作在更新缓存
            if (redisLock.lock(productId, uuid)) {
                // 从数据库中查询最新的商品库存数量
                MerInvetoryEntity productInventory = productInventoryService.findProductInventory(productId);
                logger.info("===========日志===========: 已查询到商品最新的库存数量，商品id=" + productId + ", 商品库存数量=" + productInventory.getMerCount());
                // 将最新的商品库存数量，刷新到redis缓存中去
                productInventoryService.setProductInventoryCache(productInventory);
            }
        }catch (Exception e){}finally {
            redisLock.unlock(productId, uuid);
        }
    }

    public long getProductId() {
        return productId;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

}
