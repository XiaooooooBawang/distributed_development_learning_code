package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 用阻塞队列优化秒杀业务
     *
     * @param voucherId 优惠券id
     * @return
     */
    Result seckillVoucherWithBlockingQueue(Long voucherId);

    Result seckillVoucher(Long voucherId);

    /**
     * 创建voucher订单，因为是异步处理，所以不需要返回值
     * @param voucherId
     */
    void createVoucherOrder(VoucherOrder voucherId);
}
