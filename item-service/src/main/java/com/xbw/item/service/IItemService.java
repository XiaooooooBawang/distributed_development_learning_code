package com.xbw.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xbw.item.pojo.Item;

public interface IItemService extends IService<Item> {
    void saveItem(Item item);
}
