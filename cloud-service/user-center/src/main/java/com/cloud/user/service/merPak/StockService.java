package com.cloud.user.service.merPak;

import com.cloud.user.service.merPak.IStockCallback;

import java.util.List;
import java.util.Map;

public interface StockService {

    /**
     * 从redis获取库存
     */
    public int getStock(String key);


    /**
     * 增加库存
     * @param key 库存key
     * @param expire 过期时间（秒）
     * @param num 库存数量
     * @return
     */
    public long addStock(long key, long expire, int num);


    /**
     *
     * @param key
     * @param expire
     * @param num
     * @param stockCallback
     * @return
     */
    public long stock(long key, long expire, int num, IStockCallback stockCallback);

    String locktest() throws InterruptedException;

    /**
     * 批量入库
     * @param commodityIds
     * @param i
     * @param num
     * @return
     */
    Object addBatchStock(List<Object> commodityIds, long expire, int num) throws InterruptedException;

    Object batchStock(Map<String,List<Map<String,Integer>>> map, long expire) throws InterruptedException;
}
