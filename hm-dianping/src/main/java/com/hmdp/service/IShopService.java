package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据店铺id查询店铺
     *
     * @param id 店铺id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新店铺信息
     *
     * @param shop 更新后的店铺信息
     * @return
     */
    Result update(Shop shop);
}
