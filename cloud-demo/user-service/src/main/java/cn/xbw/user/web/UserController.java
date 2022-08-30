package cn.xbw.user.web;

import cn.xbw.user.config.PatternProperties;
import cn.xbw.user.pojo.User;
import cn.xbw.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @return 用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id) {
        return userService.queryById(id);
    }
}
