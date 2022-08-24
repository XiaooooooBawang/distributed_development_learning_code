package com.xbw.redis_demo;

import com.xbw.redis_demo.util.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class RedisDemoApplicationTests {

    private Jedis jedis;

    /**
     * 建立连接
     */
    @BeforeEach
    void setUp() {
//        //1、建立连接
//        //jedis = new Jedis("172.20.10.4", 6379);
//        //2、密码
//        jedis.auth("12345678");
//        //3、选择库
//        jedis.select(0);


        //可以使用工厂设计模式来获取连接，降低耦合
        jedis = JedisConnectionFactory.getJedis();
        //选择库
        jedis.select(0);
    }

    @Test
    void testString() {
        // 存入数据
        String res = jedis.set("jedis", "test");
        System.out.println("string set res : " + res);
        //获取数据
        String name = jedis.get("name");
        System.out.println("string get name : " + name);
    }

    @Test
    void testHash() {
        // 存入hash数据
        jedis.hset("hash:jedis:1","version","1.0");
        Map<String, String> map = new HashMap<>();
        map.put("version","1.0");
        map.put("ttl","-1");
        jedis.hmset("hash:jedis:2",map);
        //获取hash数据
        Map<String, String> map1 = jedis.hgetAll("hash:jedis:1");
        Map<String, String> map2 = jedis.hgetAll("hash:jedis:2");
        System.out.println(map1);
        System.out.println(map2);
    }

    /**
     * 释放资源
     */
    @AfterEach
    void tearDown() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
