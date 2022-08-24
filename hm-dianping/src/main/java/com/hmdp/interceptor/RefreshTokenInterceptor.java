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
 * 用来刷新token的拦截器，拦截一切路径，当用户访问不需要登录的页面也能刷新token
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    /*
    因为这个拦截器类是我们为了方便在MvcConfig中添加拦截器自己写的类，没有@component等注解，
    springBoot不帮组我们管理这个类，所以不能用依赖注入，只能构造器注入，
    又因为MvcConfig归springBoot管理，所以可以在MvcConfig中注入，然后传参进拦截器的构造器实现构造器注入
    */
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token，前端是把token放入请求头中的“authorization”属性中
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //没有token，刷新不了，放行，
            return true;
        }
        //2.基于token获取redis中的用户
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        //3.判断用户是否存在
        if (userMap.isEmpty()) {
            //4.不存在，放行，让其在下一个拦截器中被拦截
            return true;
        }
        //5.将查询到的hashMap数据转换层UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6.存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //7.刷新token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户，防止内存泄漏
        UserHolder.removeUser();
    }
}
