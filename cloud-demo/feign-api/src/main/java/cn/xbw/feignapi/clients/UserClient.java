package cn.xbw.feignapi.clients;

import cn.xbw.feignapi.pojo.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 这个客户端主要是基于SpringMVC的注解来声明远程调用的信息，比如：
 * - 服务名称：userservice
 * - 请求方式：GET
 * - 请求路径：/user/{id}
 * - 请求参数：Long id
 * - 返回值类型：User
 * 这样，Feign就可以帮助我们发送http请求，无需自己使用RestTemplate来发送了。
 * feign不仅可以实现远程调用，还集成了ribbon，能对调用的服务自动实现负载均衡
 */

@FeignClient("user-service")
public interface UserClient {
    @GetMapping("/user/{id}")
    User findById(@PathVariable Long id);
}
