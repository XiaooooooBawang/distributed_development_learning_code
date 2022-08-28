package com.xbw.item.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xbw.item.pojo.Item;
import com.xbw.item.pojo.ItemStock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaffeineConfig {

    /**
     * 商品缓存
     *
     * @return
     */
    @Bean
    public Cache<Long, Item> itemCache() {
        return Caffeine.newBuilder()
                .initialCapacity(100) //缓存初始大小为100
                .maximumSize(10000) //缓存上限为10000
                .build();
    }

    /**
     * 商品库存缓存
     *
     * @return
     */
    @Bean
    public Cache<Long, ItemStock> stockCache() {
        return Caffeine.newBuilder()
                .initialCapacity(100) //缓存初始大小为100
                .maximumSize(10000) //缓存上限为10000
                .build();
    }
}
