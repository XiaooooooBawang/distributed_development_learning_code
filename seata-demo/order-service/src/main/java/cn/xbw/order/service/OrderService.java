package cn.xbw.order.service;

import cn.xbw.order.entity.Order;

public interface OrderService {

    /**
     * 创建订单
     */
    Long create(Order order);
}