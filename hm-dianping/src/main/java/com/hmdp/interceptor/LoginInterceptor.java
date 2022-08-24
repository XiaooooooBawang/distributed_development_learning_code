package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 登录拦截器，拦截需要登录的路径
 */
public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /* 用session实现校验：
        //1.获取session
        HttpSession session = request.getSession();
        //2.获取session中的用户
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        //3.判断用户是否存在
        if (userDTO == null) {
            //4.不存在，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //5.存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //6.放行
        return true;
        */

        /* 用redis实现校验，但未使用token拦截器：
        //1.获取请求头中的token，前端是把token放入请求头中的“authorization”属性中
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //token为空，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //2.基于token获取redis中的用户
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        //3.判断用户是否存在
        if (userMap.isEmpty()) {
            //4.不存在，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //5.将查询到的hashMap数据转换层UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6.存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //7.刷新token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8.放行
        return true;
        */

        //配合了刷新token的拦截器
        // 1.判断是否需要拦截（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            // 没有，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
        return true;
    }
}
