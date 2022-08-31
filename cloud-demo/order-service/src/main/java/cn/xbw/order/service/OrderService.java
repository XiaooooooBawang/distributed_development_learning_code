package cn.xbw.order.service;

import cn.xbw.feignapi.clients.UserClient;
import cn.xbw.feignapi.pojo.User;
import cn.xbw.order.mapper.OrderMapper;
import cn.xbw.order.pojo.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private UserClient userClient;

    public Order queryOrderById(Long orderId) {
        // 1.查询订单
        Order order = orderMapper.findById(orderId);
        // 2用restTemplate远程调用user-service
//        String url = "http://user-service/user/" + order.getUserId();
//        User user = restTemplate.getForObject(url, User.class);
        //2用feign远程调用user-service
        User user = userClient.findById(order.getUserId());
        //3 将user封装进order
        order.setUser(user);
        // 4.返回
        return order;
    }
}
