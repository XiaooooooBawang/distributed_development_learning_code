package cn.xbw.order;

import cn.xbw.feignapi.clients.UserClient;
import cn.xbw.feignapi.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@MapperScan("cn.xbw.order.mapper")
@SpringBootApplication
//在order-service的启动类添加注解开启Feign的功能,feign自定义配置全局生效
//@EnableFeignClients(defaultConfiguration = DefaultFeignConfig.class)

//UserClient现在在cn.xbw.feignapi.clients包下，而order-service的@EnableFeignClients注解是在cn.xbw.order包下，
// 不在同一个包，无法扫描到UserClient。所以在注入的时候spring会报错一，
// 指定feign需要加载的client接口，也可以@EnableFeignClients(basePackages = "cn.xbw.feignapi.clients")指定feign需要扫描的包
//@EnableFeignClients(clients = UserClient.class)

//多了fallback，所以不能只指定client类了，同时要指定feign的配置类（我们注册了fallback的bean）
@EnableFeignClients(basePackages = "cn.xbw.feignapi.clients", defaultConfiguration = DefaultFeignConfig.class)
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    /**
     * 用于远程调用
     *
     * @return
     */
    @Bean
    //让order-service调用user-service时搞一个负载均衡（user-service有多个实例）
    //通过ribbon实现负载均衡
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    /**
     * 自定义负载均衡策略为RandomRule随机策略，覆盖默认的ZoneAvoidanceRule
     * 但在这里这样配置的话是全局配置的，会让order-service在调用其他任何微服务的时候都用RandomRule
     * 如果想精确到调用具体某个微服务用不同的策略就要在application.yml里配置了
     *
     * @return
     */
    /*@Bean
    public IRule randomRule() {
        return new RandomRule();
    }*/

}