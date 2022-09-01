package cn.xbw.mq.helloworld;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class SpringAmqpTest {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Test
    void testSimpleQueue() {
        // 队列名称
        String queue = "simple.queue";
        // 消息
        String message = "hello, spring amqp!";
        // 发送消息
        rabbitTemplate.convertAndSend(queue, message);
    }

    @Test
    void testWorkQueue() throws InterruptedException {
        // 队列名称
        String queue = "simple.queue";
        // 消息
        String message = "hello, spring amqp!";
        for (int i = 0; i < 50; i++) {
            // 发送消息
            rabbitTemplate.convertAndSend(queue, message + i);
            Thread.sleep(20);
        }
    }

    @Test
    void testFanoutExchange() {
        // 交换机名称
        String exchangeName = "xbw.fanout";
        // 消息
        String message = "hello, fanoutQueue!";
        //只需发送到交换机就行了
        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

    @Test
    void testDirectExchange() {
        // 交换机名称
        String exchangeName = "xbw.direct";
        // 消息
        String message = "hello, directQueue!";
        //只需发送到交换机就行了
        rabbitTemplate.convertAndSend(exchangeName, "blue", message);
    }

    @Test
    void testTopicExchange() {
        // 交换机名称
        String exchangeName = "xbw.topic";
        // 消息
        String message = "hello, topicQueue!";
        //只需发送到交换机就行了
        rabbitTemplate.convertAndSend(exchangeName, "china.news", message);
    }
}
