package com.cloud.user.service;

/**
 * 初始化库存时调用，从数据库获取库存
 */
public interface IStockCallback {

    /**
     * 获取库存
     * @return
     */
    int getStock();
}
