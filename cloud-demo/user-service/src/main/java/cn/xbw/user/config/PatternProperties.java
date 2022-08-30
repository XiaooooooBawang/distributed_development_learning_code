package cn.xbw.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 让nacos的配置能热更新有两种,方法二：使用@ConfigurationProperties注解代替@Value注解.
 * 推荐用这种
 */
@Data
@Component
@ConfigurationProperties(prefix = "pattern")
public class PatternProperties {
    private String dateformat;
    private String envSharedValue;
}
