package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() throws IOException {
        //配置
        /*//这种是写死的，不太好
        Config config = new Config();
        config.useSingleServer().setAddress("redis://172.20.10.4:6379").setPassword("12345678");*/

        Config config = Config.fromYAML(RedissonConfig.class.getClassLoader().getResource("redisson-config.yaml"));
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
