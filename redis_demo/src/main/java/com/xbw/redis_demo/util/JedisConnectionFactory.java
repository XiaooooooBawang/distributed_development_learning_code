package com.xbw.redis_demo.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * 工厂设计模式
 */
public class JedisConnectionFactory {
    public static final JedisPool JEDIS_POOL;

    /**
     * 连接池初始化
     * 静态代码块：随着类的加载而加载，确保只能执行一次，我们在加载当前工厂类的时候，就可以执行static的操作完成对 连接池的初始化
     */
    static {
        //链接池配置类
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        /*
        可以配置各种参数
        源码：默认配置
        public static final int DEFAULT_MAX_TOTAL = 8;
        public static final int DEFAULT_MAX_IDLE = 8;
        public static final int DEFAULT_MIN_IDLE = 0;
        private int maxTotal = 8;   最大连接数
        private int maxIdle = 8;   最大空闲连接数
        private int minIdle = 0;   最小空闲连接数
        配的都是默认参数，写不写都一样
        */
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);

        //创建数据库连接池
        //注意这里的账户名，工厂模式的话用xbw（centos的用户名）是可以的，但redis的用户名默认是default（我们没改过），所以用default也是可以的
        //但下一个模块中用springDataRedis中的配置文件username是不能用xbw的，只能写default或不写
        JEDIS_POOL = new JedisPool("172.20.10.4", 6379, "default", "12345678");
    }
    /**
     * 获取连接池中的连接
     */
    public static Jedis getJedis() {
        return JEDIS_POOL.getResource();
    }
}
