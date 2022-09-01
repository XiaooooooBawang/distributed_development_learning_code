package cn.xbw.mq.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageConverterConfig {
    /**
     * 自定义配置消息转换器，覆盖默认的。Spring会把你发送的消息序列化为字节发送给MQ，接收消息的时候，还会把字节反序列化为Java对象。
     * 默认情况下Spring采用的序列化方式是JDK序列化,JDK序列化存在下列问题：
     * - 数据体积过大
     * - 有安全漏洞
     * - 可读性差
     * 因此可以使用JSON方式来做序列化和反序列化.
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
