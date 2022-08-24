package com.xbw.redis_spring_data_redis.domain;

import lombok.Data;

@Data
public class User {
    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    //要有一个无参构造函数，因为测试中取对象强转成User，需要无参构造函数，如果不强转的话可以不写无参构造函数
    public User() {
    }
}
