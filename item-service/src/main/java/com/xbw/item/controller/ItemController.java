package com.xbw.item.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.xbw.item.pojo.Item;
import com.xbw.item.pojo.ItemStock;
import com.xbw.item.pojo.PageDTO;
import com.xbw.item.service.IItemService;
import com.xbw.item.service.IItemStockService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("item")
public class ItemController {

    @Resource
    private IItemService itemService;
    @Resource
    private IItemStockService stockService;
    @Resource
    private Cache<Long, Item> itemCache;
    @Resource
    private Cache<Long, ItemStock> stockCache;

    @GetMapping("list")
    public PageDTO queryItemPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size){
        // 分页查询商品
        Page<Item> result = itemService.query()
                .ne("status", 3)
                .page(new Page<>(page, size));

        // 查询库存
        List<Item> list = result.getRecords().stream().peek(item -> {
            ItemStock stock = stockService.getById(item.getId());
            item.setStock(stock.getStock());
            item.setSold(stock.getSold());
        }).collect(Collectors.toList());

        // 封装返回
        return new PageDTO(result.getTotal(), list);
    }

    @PostMapping
    public void saveItem(@RequestBody Item item){
        itemService.saveItem(item);
    }

    @PutMapping
    public void updateItem(@RequestBody Item item) {
        itemService.updateById(item);
    }

    @PutMapping("stock")
    public void updateStock(@RequestBody ItemStock itemStock){
        stockService.updateById(itemStock);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable("id") Long id){
        itemService.update().set("status", 3).eq("id", id).update();
    }

    @GetMapping("/{id}")
    public Item findById(@PathVariable("id") Long id){
        //先在本地缓存中查，未命中就去数据库中查
        return itemCache.get(id, key -> itemService.query()
                                        .ne("status", 3).eq("id", key)
                                        .one()
        );
    }

    @GetMapping("/stock/{id}")
    public ItemStock findStockById(@PathVariable("id") Long id){
        //先在本地缓存中查，未命中就去数据库中查
        return stockCache.get(id, key -> stockService.getById(key));
    }
}
