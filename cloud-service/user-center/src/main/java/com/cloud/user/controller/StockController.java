package com.cloud.user.controller;

import com.cloud.model.user.model.MerInvetoryEntity;
import com.cloud.user.dao.StockDao;
import com.cloud.user.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class StockController {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockDao stockDao;



    private int initStock(long commodityId) {
        MerInvetoryEntity entity = stockDao.findById(commodityId);
        return entity!=null?entity.getMerCount():0;
    }
    @RequestMapping(value = "stock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object stock(@RequestParam(value = "commodityId")long commodityId,@RequestParam(value = "num")int num) {
        long stock = stockService.stock(commodityId, 60 * 60, num, () -> initStock(commodityId));
        return stock >= 0;
    }


    @RequestMapping(value = "getStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getStock(@RequestParam(value = "commodityId")long commodityId) {
        // 商品ID
//        long commodityId = 1;
        // 库存ID
        String redisKey = String.valueOf(commodityId);

        return stockService.getStock(redisKey);
    }



    @RequestMapping(value = "addStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object addStock(@RequestParam(value = "commodityId")long commodityId,@RequestParam(value = "num")int num) {
        // 商品ID
//        long commodityId = 2;
        // 库存ID
        long redisKey = commodityId;

        return stockService.addStock(redisKey, 60 * 60,num);
    }

    @RequestMapping("/locktest")
    @ResponseBody
    public String locktest() throws InterruptedException {
        return stockService.locktest();
    }


    @RequestMapping(value = "addBatchStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object addBatchStock(@RequestBody Map<String,Object> map) throws InterruptedException {
        // 商品ID list  commodityId    num
        List<Object> commodityIds = (List<Object>) map.get("commodityIds");
        int num =  Integer.valueOf((String) map.get("num"));
        return stockService.addBatchStock(commodityIds, 60 * 60,num);
    }

    @RequestMapping(value = "batchStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object batchStock(@RequestBody Map<String,List<Map<String,Integer>>> map) throws InterruptedException {
        return stockService.batchStock(map, 60 * 60);
    }
}