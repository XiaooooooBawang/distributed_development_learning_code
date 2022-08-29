package com.xbw.item.canal;


import com.github.benmanes.caffeine.cache.Cache;
import com.xbw.item.config.RedisHandler;
import com.xbw.item.pojo.Item;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import javax.annotation.Resource;

/**
 * 监听器
 */
@Component
@CanalTable("tb_item") //需要监听的表名
public class ItemHandler implements EntryHandler<Item> {
    /*
    这里没有更新Nginx，Nginx的缓存更新策略就是过期时间，
    业务上也没太大必要专门在Nginx上搞太好的更新（因为要用lua写，比较麻烦），所以Nginx放入的缓存就尽量不要放时效性很高的数据。
    当然，想折腾也行
     */

    @Resource
    private RedisHandler redisHandler;

    @Resource
    private Cache<Long, Item> itemCache;

    @Override
    public void insert(Item item) {
        // 写数据到JVM进程缓存
        itemCache.put(item.getId(), item);
        // 写数据到redis
        redisHandler.saveItem(item);
    }

    @Override
    public void update(Item before, Item after) {
        // 写数据到JVM进程缓存
        itemCache.put(after.getId(), after);
        // 写数据到redis
        redisHandler.saveItem(after);
    }

    @Override
    public void delete(Item item) {
        // 删除数据到JVM进程缓存
        itemCache.invalidate(item.getId());
        // 删除数据到redis
        redisHandler.deleteItemById(item.getId());
    }
}
