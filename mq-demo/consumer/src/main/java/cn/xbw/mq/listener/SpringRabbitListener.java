package cn.xbw.mq.listener;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;


/**
 * 消费者监听队列
 */
@Component
public class SpringRabbitListener {

//    @RabbitListener(queues = "simple.queue")
//    public void listenSimpleQueueMessage(String msg) {
//        System.out.println("spring 消费者接收到消息：【" + msg + "】");
//    }

    @RabbitListener(queues = "simple.queue")
    public void listenWorkQueueMessage(String msg) throws InterruptedException {
        System.out.println("spring 消费者1接收到消息：【" + msg + "】" + LocalTime.now());
        Thread.sleep(20);
    }

    @RabbitListener(queues = "simple.queue")
    public void listenWorkQueueMessage2(String msg) throws InterruptedException {
        System.out.println("spring 消费者2   接收到消息：【" + msg + "】" + LocalTime.now());
        Thread.sleep(200);
    }

    @RabbitListener(queues = "fanout.queue1")
    public void listenFanoutQueue1(String msg) {
        System.out.println("spring 消费者接收fanoutQueue1到消息：【" + msg + "】");
    }

    @RabbitListener(queues = "fanout.queue2")
    public void listenFanoutQueue2(String msg) {
        System.out.println("spring 消费者接收fanoutQueue2到消息：【" + msg + "】");
    }

    /**
     * 直接在@RabbitListener注解中就能声明交换机，交换机类型，队列，key，绑定关系，不用再用bean声明
     *
     * @param msg
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("direct.queue1"),
            exchange = @Exchange(name = "xbw.direct", type = ExchangeTypes.DIRECT),
            key = {"red", "blue"}
    ))
    public void listenDirectQueue1(String msg) {
        System.out.println("spring 消费者接收directQueue1到消息：【" + msg + "】");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("direct.queue2"),
            exchange = @Exchange(name = "xbw.direct", type = ExchangeTypes.DIRECT),
            key = {"red", "yellow"}
    ))
    public void listenDirectQueue2(String msg) {
        System.out.println("spring 消费者接收directQueue2到消息：【" + msg + "】");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("topic.queue1"),
            exchange = @Exchange(name = "xbw.topic", type = ExchangeTypes.TOPIC),
            //Topic交换机接收的消息RoutingKey必须是多个单词，以 `**.**` 分割
            //Topic交换机与队列绑定时的bindingKey可以指定通配符
            // 通配符规则：
            //`#`：匹配一个或多个词
            //`*`：匹配不多不少恰好1个词
            key = "china.#"
    ))
    public void listenTopicQueue1(String msg) {
        System.out.println("spring 消费者接收topicQueue1到消息：【" + msg + "】");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("topic.queue2"),
            exchange = @Exchange(name = "xbw.topic", type = ExchangeTypes.TOPIC),
            key = "#.news"
    ))
    public void listenTopicQueue2(String msg) {
        System.out.println("spring 消费者接收topicQueue2到消息：【" + msg + "】");
    }
}
