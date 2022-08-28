package com.xbw.test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class CaffeineTest {

    /*
      基本用法测试
     */
    @Test
    void testBasicOps() {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder().build();

        // 存数据
        cache.put("xbw","666");

        // 取数据，不存在则返回null
        String xbw = cache.getIfPresent("xbw");
        System.out.println("xbw = " + xbw);

        // 取数据，不存在则去数据库查询
        // 参数一：缓存的key
        // 参数二：Lambda表达式，表达式参数就是缓存的key，方法体是查询数据库的逻辑
        // 优先根据key查询JVM缓存，如果未命中，则执行参数二的Lambda表达式
        String sqlXbw = cache.get("sqlXbw", key -> {
            //根据可以去数据库查数据，这里就直接返回888算了
            return "888";
        });
        System.out.println("sqlXbw = " + sqlXbw);

    }

    /*
     基于大小设置驱逐策略：
     */
    @Test
    void testEvictByNum() throws InterruptedException {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder()
                // 设置缓存大小上限为 1
                .maximumSize(1)
                .build();
        // 存数据
        cache.put("gf1", "柳岩");
        cache.put("gf2", "范冰冰");
        cache.put("gf3", "迪丽热巴");

        /*注意：在默认情况下，当一个缓存元素过期的时候，Caffeine不会自动立即将其清理和驱逐。
        而是在一次读或写操作后，或者在空闲时间完成对失效数据的驱逐。*/

        // 延迟10ms，给清理线程一点时间
        Thread.sleep(10L);
        // 获取数据
        System.out.println("gf1: " + cache.getIfPresent("gf1"));
        System.out.println("gf2: " + cache.getIfPresent("gf2"));
        System.out.println("gf3: " + cache.getIfPresent("gf3"));
    }

    /*
     基于时间设置驱逐策略：
     */
    @Test
    void testEvictByTime() throws InterruptedException {
        // 创建缓存对象
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(1)) // 设置缓存有效期为 10 秒
                .build();
        // 存数据
        cache.put("gf", "柳岩");
        // 获取数据
        System.out.println("gf: " + cache.getIfPresent("gf"));
        // 休眠1.2秒
        Thread.sleep(1200L);
        System.out.println("gf: " + cache.getIfPresent("gf"));
    }
}
