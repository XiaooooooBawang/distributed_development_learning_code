package com.hmdp.utils;

/**
 * 锁的基本接口
 */
public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true代表获取锁成功；false代表获取锁失败
     */
    boolean getLock(long timeoutSec);

    /**
     * 使用lua脚本来释放锁，实现获取锁、比较锁和删除锁的操作的原子性，防止比较锁和删除锁之间出现阻塞，导致多线程情况下出现误删锁
     */
    void unlockWithLua();

    /**
     * 释放锁，没有使用lua脚本
     */
    void unlock();

}
