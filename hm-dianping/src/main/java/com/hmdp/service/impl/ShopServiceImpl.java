package com.hmdp.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据店铺id查询店铺
     *
     * @param id 店铺id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透的查询:
        //Shop shop = queryWithPassThrough(id);
//        Shop shop = cacheClient
//                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //互斥锁解决缓存击穿（不是缓存穿透）
        //Shop shop = queryWithMutexLock(id);


        //逻辑过期解决缓存击穿
        //Shop shop = queryWithLogicExpire(id);
        Shop shop = cacheClient
                .queryWithLogicExpire(
                        CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 更新店铺信息
     * 注意要添加 @Transactional 事务注解
     *
     * @param shop 更新后的店铺信息
     * @return
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {

            return Result.fail("店铺id不能为空");
        }
        //1.先更新数据库
        updateById(shop);
        //2.再更新缓存
        String key = CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    /**
     * 缓存重建执行器，分配10个线程的线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期 解决缓存击穿
     * 因为缓存中未命中直接返回空，所以不用担心缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryWithLogicExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
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
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 3.1.未过期，直接返回店铺信息
            return shop;
        }
        // 3.2.已过期，需要缓存重建
        // 4.缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = getLock(lockKey);
        // 4.2.判断是否获取锁成功
        if (isLock) {
            //4.3成功，开启独立线程进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        //4.4成不成功,该线程都返回过期的店铺信息
        return shop;
    }


    /**
     * 互斥锁解决缓存击穿的封装类，因为在queryWithPassThrough类的基础上修改而来，所以能解决缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryWithMutexLock(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1,从redis查询商铺缓存
        //这里应该用hash比较好，但也写一写string，练习json的处理
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断redis中是否存在
        if (CharSequenceUtil.isNotBlank(shopJson)) {
            //3.redis中存在，反序列化为对象后直接返回对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //return Result.ok(shop);
            return shop;
        }
        //判断是否为空对象，防止缓存击穿(上面的if判断为blank的是null和空字符串“”，我们防止缓存穿透时缓存的是空字符串"")
        if (shopJson != null) {
            //return Result.fail("（防缓存击穿）店铺不存在");
            return null;
        }

        // 4.实现缓存重构
        //4.1 获取互斥锁
        String LockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = getLock(LockKey);
            // 4.2 判断否获取成功
            if (!isLock) {
                //4.3 失败，则休眠重试
                Thread.sleep(50);
                return queryWithMutexLock(id);
            }
            //4.4 成功，根据id查询数据库
            shop = getById(id);
            //模拟复杂重构的延时
            Thread.sleep(200);
            //5.判断数据库中是否存在
            if (shop == null) {
                //5.1.数据库中不存在，把空字符串写入redis，防止缓存穿透，同时设置超时时间要短些，2分钟（虽然名字叫缓存空对象，但不能真的传null，redis是不会进行这个操作的）
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                //返回错误信息
                //return Result.fail("店铺不存在");
                return null;
            }
            //5.2.数据库中存在，写入redis，并设置超时时间为30分钟，实现超时剔除
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //6.释放互斥锁
            unlock(LockKey);
        }
        //7.返回
        //return Result.ok(shop);
        return shop;
    }


    /**
     * 获取互斥锁，互斥锁用redis中的setnx的效果来实现
     *
     * @param lockKey
     * @return
     */
    private boolean getLock(String lockKey) {
        //如果不存在key才能set,超时时间为10秒
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁
     *
     * @param lockKey
     */
    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    /**
     * 将店铺信息存进redis中，进行缓存预热，用单元测试来实现
     *
     * @param id            店铺id
     * @param expireSeconds 逻辑过期时间
     */
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //1.查询店铺数据
        Shop shop = getById(id);
        //模拟复杂重构的延迟时间
        Thread.sleep(200);
        //2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        redisData.setData(shop);
        //3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 解决缓存穿透的封装类（笔记类）
     *
     * @param id 店铺id
     * @return
     */
    public Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1,从redis查询商铺缓存
        //这里应该用hash比较好，但也写一写string，练习json的处理
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断redis中是否存在
        if (CharSequenceUtil.isNotBlank(shopJson)) {
            //3.redis中存在，反序列化为对象后直接返回对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //return Result.ok(shop);
            return shop;
        }
        //判断是否为空对象，防止缓存击穿(上面的if判断为blank的是null和空字符串“”，我们防止缓存穿透时缓存的是空字符串"")
        if (shopJson != null) {
            //return Result.fail("（防缓存击穿）店铺不存在");
            return null;
        }
        //4.redis中不存在，根据id查询数据库
        Shop shop = getById(id);
        //5.判断数据库中是否存在
        if (shop == null) {
            //6.数据库中不存在，把空字符串写入redis，防止缓存穿透，同时设置超时时间要短些，2分钟（虽然名字叫缓存空对象，但不能真的传null，redis是不会进行这个操作的）
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //返回错误信息
            //return Result.fail("店铺不存在");
            return null;
        }
        //7.数据库中存在，写入redis，并设置超时时间为30分钟，实现超时剔除
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //8.返回
        //return Result.ok(shop);
        return shop;
    }
}
