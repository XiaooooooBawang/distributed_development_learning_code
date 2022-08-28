package com.xbw.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xbw.item.mapper.ItemMapper;
import com.xbw.item.pojo.Item;
import com.xbw.item.pojo.ItemStock;
import com.xbw.item.service.IItemService;
import com.xbw.item.service.IItemStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class ItemService extends ServiceImpl<ItemMapper, Item> implements IItemService {
    @Resource
    private IItemStockService stockService;
    @Override
    @Transactional
    public void saveItem(Item item) {
        // 新增商品
        save(item);
        // 新增库存
        ItemStock stock = new ItemStock();
        stock.setId(item.getId());
        stock.setStock(item.getStock());
        stockService.save(stock);
    }
}
