package cn.xbw.user.web;

import cn.xbw.user.config.PatternProperties;
import cn.xbw.user.pojo.User;
import cn.xbw.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/user")
//@RefreshScope  //让nacos的配置能热更新有两种,方法一：在@Value注入的变量所在类上添加注解@RefreshScope
public class UserController {

    @Resource
    private UserService userService;

//    @Value("${pattern.dateformat}")
//    private String dateformat;

    @Resource
    private PatternProperties patternProperties;

    /**
     * 用来验证nacos配置管理
     *
     * @return
     */
    @GetMapping("now")
    public String now(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDateformat()));
    }


    /**
     * 用来验证nacos多环境共享配置
     * @return
     */
    @GetMapping("prop")
    public PatternProperties prop() {
        return patternProperties;
    }

    /**
     * 路径： /user/110
     *
     * @param id 用户id
     * @param author 请求头中的author权限，因为传递的参数可能为空值（没有提交这个参数），在用feign调用（order调user）的时候并不会提交
     *               这个author参数，API会抛异常，报400，bad request，所以RequestHeader 要加上 required = false，可以要求不一定提交
     *
     * @return 用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id, @RequestHeader(value = "author", required = false) String author) {
        System.out.println("author = " + author);  //验证一下gateway中的过滤器
        return userService.queryById(id);
    }
}
