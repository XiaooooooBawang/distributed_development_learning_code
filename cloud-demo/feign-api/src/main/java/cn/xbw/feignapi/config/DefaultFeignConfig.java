package cn.xbw.feignapi.config;

import cn.xbw.feignapi.clients.fallback.UserClientFallbackFactory;
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
        return Logger.Level.FULL;
    }

    // 将UserClientFallbackFactory注册为一个Bean，
    // 记得要在order-service的启动类的@EnableFeignClients中指定feign的配置类（我们注册了fallback的bean）
    @Bean
    public UserClientFallbackFactory userClientFallbackFactory() {
        return new UserClientFallbackFactory();
    }
}
