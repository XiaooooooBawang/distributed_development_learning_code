package cn.xbw.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@MapperScan("cn.xbw.order.mapper")
@SpringBootApplication
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