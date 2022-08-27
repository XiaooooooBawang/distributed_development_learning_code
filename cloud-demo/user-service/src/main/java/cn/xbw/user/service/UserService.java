package cn.xbw.user.service;

import cn.xbw.user.mapper.UserMapper;
import cn.xbw.user.pojo.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    public User queryById(Long id) {
        return userMapper.findById(id);
    }
}