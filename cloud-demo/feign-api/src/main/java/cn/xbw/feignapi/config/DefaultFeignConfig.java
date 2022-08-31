package cn.xbw.feignapi.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;


/**
 * 如果要**全局生效**，将其放到启动类的@EnableFeignClients这个注解中：
 * @EnableFeignClients(defaultConfiguration = DefaultFeignConfiguration .class)
 *
 * 如果是**局部生效**，则把它放到对应的@FeignClient这个注解中：
 * @FeignClient(value = "userservice", configuration = DefaultFeignConfiguration .class)
 */
public class DefaultFeignConfig {
    //注意是feign的包
    @Bean
    public Logger.Level logLevel() {
        return Logger.Level.NONE;
    }
}
