package com.xbw.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xbw.item.service.IItemStockService;
import com.xbw.item.mapper.ItemStockMapper;
import com.xbw.item.pojo.ItemStock;
import org.springframework.stereotype.Service;

@Service
public class ItemStockService extends ServiceImpl<ItemStockMapper, ItemStock> implements IItemStockService {
}
