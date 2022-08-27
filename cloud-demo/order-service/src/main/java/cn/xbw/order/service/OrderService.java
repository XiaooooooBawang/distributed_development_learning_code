package cn.xbw.order.service;

import cn.xbw.order.mapper.OrderMapper;
import cn.xbw.order.pojo.Order;
import cn.xbw.order.pojo.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    public RestTemplate restTemplate;

    public Order queryOrderById(Long orderId) {
        // 1.查询订单
        Order order = orderMapper.findById(orderId);
        // 2远程查询user
        String url = "http://user-service/user/" + order.getUserId();
        User user = restTemplate.getForObject(url, User.class);
        //3 将user封装进order
        order.setUser(user);
        // 4.返回
        return order;
    }
}
