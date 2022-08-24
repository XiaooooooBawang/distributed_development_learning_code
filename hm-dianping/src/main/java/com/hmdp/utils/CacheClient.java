package com.hmdp.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * 基于StringRedisTemplate封装一个redis缓存工具类
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     *
     * @param key   键
     * @param value java对象
     * @param time  时间长度
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
     *
     * @param key   键
     * @param value java对象
     * @param time  时间长度
     * @param unit  时间单位
     */
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallBack
     * @param time
     * @param unit
     * @param <R>
     * @param <T>
     * @return
     */
    public <R, T> R queryWithPassThrough(
            String keyPrefix, T id, Class<R> type, Function<T, R> dbFallBack, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1,从redis查询商铺缓存
        //这里应该用hash比较好，但也写一写string，练习json的处理
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断redis中是否存在
        if (CharSequenceUtil.isNotBlank(json)) {
            //3.redis中存在，反序列化为对象后直接返回对象
            return JSONUtil.toBean(json, type);
        }
        //判断是否为空对象，防止缓存击穿(上面的if判断为blank的是null和空字符串“”，我们防止缓存穿透时缓存的是空字符串"")
        if (json != null) {
            return null;
        }
        //4.redis中不存在，根据id查询数据库
        R r = dbFallBack.apply(id);
        //5.判断数据库中是否存在
        if (r == null) {
            //6.数据库中不存在，把空字符串写入redis，防止缓存穿透，同时设置超时时间要短些，2分钟（虽然名字叫缓存空对象，但不能真的传null，redis是不会进行这个操作的）
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //7.数据库中存在，写入redis，并设置超时时间为30分钟，实现超时剔除
        this.set(key, r, time, unit);
        //8.返回
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
     *
     * @param keyPrefix
     * @param lockPrefix
     * @param id
     * @param type
     * @param dbFallBack
     * @param time
     * @param unit
     * @param <R>
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public <R, T> R queryWithLogicExpire(
            String keyPrefix, String lockPrefix, T id, Class<R> type, Function<T, R> dbFallBack, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1,从redis查询商铺缓存
        //这里应该用hash比较好，但也写一写string，练习json的处理
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断redis中是否存在
        if (CharSequenceUtil.isBlank(json)) {
            //2.1如果缓存未命中，直接返回空
            return null;
        }
        // 2.2.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 3.1.未过期，直接返回店铺信息
            return r;
        }
        // 3.2.已过期，需要缓存重建
        // 4.缓存重建
        // 4.1.获取互斥锁
        String lockKey = lockPrefix + id;
        boolean isLock = getLock(lockKey);
        // 4.2.判断是否获取锁成功
        if (isLock) {
            //4.3成功，开启独立线程进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R r1 = dbFallBack.apply(id);
                    //写入redis
                    this.setWithLogicExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        //4.4成不成功,该线程都返回过期的店铺信息
        return r;
    }

    private boolean getLock(String lockKey) {
        //如果不存在key才能set,超时时间为10秒
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }
}
