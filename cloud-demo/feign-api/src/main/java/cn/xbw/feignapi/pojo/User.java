package cn.xbw.feignapi.pojo;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String address;
}