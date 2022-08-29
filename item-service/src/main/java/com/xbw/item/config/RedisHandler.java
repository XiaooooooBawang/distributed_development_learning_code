package com.xbw.item.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbw.item.pojo.Item;
import com.xbw.item.pojo.ItemStock;
import com.xbw.item.service.IItemService;
import com.xbw.item.service.IItemStockService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class RedisHandler implements InitializingBean {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IItemService iItemService;

    @Resource
    private IItemStockService iItemStockService;

    /**
     * 一个处理json相关的工具
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 利用InitializingBean接口来实现，因为InitializingBean可以在对象被Spring创建并且成员变量全部注入后执行
     * 用来缓存预热
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 1.查询所有商品信息（缓存预热应该放的是热点信息，但这里数据量比较小，全部都放进缓存）
        List<Item> itemList = iItemService.list();
        // 2.放入缓存
        for (Item item : itemList) {
            // 2.1.item序列化为JSON
            String json = MAPPER.writeValueAsString(item);
            // 2.2.存入redis
            stringRedisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        }

        // 3.查询所有商品库存信息
        List<ItemStock> itemStockList = iItemStockService.list();
        // 4.放入缓存
        for (ItemStock itemStock : itemStockList) {
            // 4.1.itemStock序列化为JSON
            String json = MAPPER.writeValueAsString(itemStock);
            // 4.2.存入redis
            stringRedisTemplate.opsForValue().set("item:stock:id:" + itemStock.getId(), json);
        }
    }

    /*
    封装一下保存和删除操作，方便缓存同步使用
     */
    public void saveItem(Item item) {
        try {
            String json = MAPPER.writeValueAsString(item);
            stringRedisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteItemById(Long id) {
        stringRedisTemplate.delete("item:id:" + id);
    }
}
