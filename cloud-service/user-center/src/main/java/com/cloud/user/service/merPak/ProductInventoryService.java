package com.cloud.user.service.merPak;

import com.cloud.model.user.model.MerInvetoryEntity;

public interface ProductInventoryService {
    /**
     * 从数据库中查询最新的商品库存数量
     * @param productId 商品id
     * @return
     */
    MerInvetoryEntity findProductInventory(long productId);

    /**
     * 将最新的商品库存数量，刷新到redis缓存中去
     * @param productInventory
     */
    void setProductInventoryCache(MerInvetoryEntity productInventory);

    /**
     * 删除redis中的缓存
     * @param productInventory
     */
    void removeProductInventoryCache(MerInvetoryEntity productInventory);

    /**
     * 修改数据库中的库存
     * @param productInventory
     */
    void updateProductInventory(MerInvetoryEntity productInventory);

    /**
     * 尝试去redis中读取一次商品库存的缓存数据
     * @param productId
     * @return
     */
    MerInvetoryEntity getProductInventoryCache(long productId);
}
