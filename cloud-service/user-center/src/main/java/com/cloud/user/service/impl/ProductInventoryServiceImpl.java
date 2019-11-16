package com.cloud.user.service.impl;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.dao.StockDao;
import com.cloud.user.service.ProductInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ProductInventoryServiceImpl implements ProductInventoryService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String KRY_PRE = "redis_key:stock:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private StockDao stockDao;

    /**
     * 从数据库中查询最新的商品库存数量
     * @param productId 商品id
     * @return
     */
    @Override
    public MerInvetoryEntity findProductInventory(long productId) {
        return stockDao.findById(productId);
    }

    /**
     * 将最新的商品库存数量，刷新到redis缓存中去
     * @param productInventory
     */
    @Override
    public void setProductInventoryCache(MerInvetoryEntity productInventory) {
        String key = KRY_PRE + productInventory.getMerUid();
        redisTemplate.opsForValue().set(key,productInventory.getMerCount(),60 * 60 ,TimeUnit.SECONDS);
    }

    /**
     * 删除redis中的缓存
     * @param productInventory
     */
    @Override
    public void removeProductInventoryCache(MerInvetoryEntity productInventory) {
        String key = KRY_PRE + productInventory.getMerUid();
        redisTemplate.delete(key);
        logger.info("===========日志===========: 已删除redis中的缓存，key=" + key);
    }

    /**
     * 修改数据库中的库存
     * @param productInventory
     */
    @Override
    public void updateProductInventory(MerInvetoryEntity productInventory) {
        stockDao.updateProductInventory(productInventory);
        logger.info("===========日志===========: 已修改数据库中的库存，商品id=" + productInventory.getMerUid() + ", 商品库存数量=" + productInventory.getMerCount());
    }

    /**
     * 尝试去redis中读取一次商品库存的缓存数据
     * @param productId
     * @return
     */
    @Override
    public MerInvetoryEntity getProductInventoryCache(long productId) {
        int inventoryCnt = 0;

        String key = KRY_PRE + productId;
        int result = (int) redisTemplate.opsForValue().get(key);

        if(result > 0) {
            try {
                inventoryCnt = result;
                return new MerInvetoryEntity(productId, inventoryCnt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
