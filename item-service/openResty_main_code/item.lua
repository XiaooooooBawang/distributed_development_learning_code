-- 引入自定义common工具模块，返回值是common中返回的 _M
-- 利用require('....')来导入该函数库
local common = require('common')
local read_redis = common.read_redis
-- 从 common中获取read_http这个函数
local read_http = common.read_http
-- 导入cjson库
local cjson = require('cjson')
-- 获取本地缓存对象
local item_cache = ngx.shared.item_cache

-- 封装查询函数（查询缓存顺序为openResty的Nginx本地缓存，redis，tomcat）
function read_data(key, expire, path, params)
    -- 查询openResty的Nginx本地缓存
    local val = item_cache:get(key)
    if not val then
        ngx.log(ngx.ERR, "本地缓存查询失败，尝试查询Redis， key: ", key)
        -- 查询redis缓存,这里redis和openResty为同一台机器
        val = read_redis("127.0.0.1", 6379, key)
        -- 判断查询结果
        if not val then
            ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", key)
            -- redis查询失败，去查询http（tomcat）
            val = read_http(path, params)
        end 
    end
    -- 查询成功，把数据写入本地缓存,在外面写存储，可以更新有效时间
    -- 存储, 指定key、value、过期时间，单位s，默认为0代表永不过期
    item_cache:set(key, val, expire)
    -- 返回数据
    return val
end

-- 获取请求路径参数
local id = ngx.var[1]
-- 根据id查询商品
local itemJson = read_data('item:id:' .. id ,1800,'/item/'.. id, nil)
-- 根据id查询商品库存
local itemStockJson = read_data('item:stock:id:' .. id ,60,'/item/stock/'..id,nil)
-- 把JSON变为lua的table，完成数据整合后，再转为JSON
local item = cjson.decode(itemJson)
local stock = cjson.decode(itemStockJson)
item.stock = stock.stock
item.sold = stock.sold
ngx.say(cjson.encode(item))
