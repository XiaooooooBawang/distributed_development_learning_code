package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 业务名，方便标识不同业务的锁
     */
    private final String serviceName;

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String serviceName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.serviceName = serviceName;
    }

    private static final String KEY_PREFIX = "lock:";

    /**
     * 定义lua脚本常量，并用静态代码块初始化
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 线程id的前缀，线程id由jvm维护，集群模式下只用线程id会冲突
     * 用hutool的uuid类，toString(true)能把uuid中的横线去掉
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";


    /**
     * 利用setnx方法进行加锁，同时增加过期时间，防止死锁
     *
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return
     */
    @Override
    public boolean getLock(long timeoutSec) {
        // 获取线程标示（uuid+线程id）
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁，存该线程的线程标识
        Boolean isSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + serviceName, threadId, timeoutSec, TimeUnit.SECONDS);
        //tips：若存在自动拆箱操作，要考虑会不会出现NEP空指针异常，包括其他一些情景也要考虑判空，养成良好的习惯
        return Boolean.TRUE.equals(isSuccess);
    }

    /**
     * 使用lua脚本来释放锁，实现获取锁、比较锁和删除锁的操作的原子性，防止比较锁和删除锁之间出现阻塞，导致多线程情况下出现误删锁
     */
    @Override
    public void unlockWithLua() {
        //执行lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + serviceName),
                ID_PREFIX + Thread.currentThread().getId());
    }

    /**
     * 释放锁，没有使用lua脚本
     */
    @Override
    public void unlock() {
        //锁的key
        String lockKey = KEY_PREFIX + serviceName;
        // 获取线程标示（uuid+线程id）
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁中标识
        String id = stringRedisTemplate.opsForValue().get(lockKey);
        //判断标识是否一致
        if (threadId.equals(id)) {
            //一致才能释放锁，防止误删了另外一个线程的锁
            stringRedisTemplate.delete(lockKey);
        }
    }
}
