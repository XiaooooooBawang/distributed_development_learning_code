package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在原本domain上添加逻辑过期时间的对象类
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
