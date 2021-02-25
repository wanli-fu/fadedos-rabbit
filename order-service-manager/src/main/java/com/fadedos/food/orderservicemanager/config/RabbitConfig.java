package com.fadedos.food.orderservicemanager.config;

import com.fadedos.food.orderservicemanager.service.OrderMessageService;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Configuration
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;

    @Autowired //该方法会被自动在spring boot启动时调用
    public void startListenMessage() throws InterruptedException, TimeoutException, IOException {
        orderMessageService.handleMessage();
    }

    @Autowired
    public void initRabbit() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        /*---------restaurant---------*/
        //声明交换机
        Exchange exchange = new DirectExchange("exchange.order.restaurant");
        rabbitAdmin.declareExchange(exchange);

        //声明队列
        Queue queue = new Queue("queue.order");
        rabbitAdmin.declareQueue(queue);

        //绑定队列
        Binding binding = new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.restaurant",
                "key.order",
                null);
        rabbitAdmin.declareBinding(binding);

        /*---------deliveryman---------*/
        //声明交换机
        exchange = new DirectExchange("exchange.order.deliveryman");
        rabbitAdmin.declareExchange(exchange);

        //绑定队列
        binding = new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.deliveryman",
                "key.order",
                null);
        rabbitAdmin.declareBinding(binding);

        /*---------settlement---------*/
        //声明交换机
        //由于使用的fanout 群发 则须用两个虚拟机  订单模块 给 结算模块发送的消息 是这个交换机
        exchange = new FanoutExchange("exchange.order.settlement");
        rabbitAdmin.declareExchange(exchange);

        exchange = new FanoutExchange("exchange.settlement.order");
        rabbitAdmin.declareExchange(exchange);

        //绑定队列
        binding = new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                //由于使用的fanout 群发 则须用两个虚拟机  订单模块 接收的到 结算模块的消息 是这个交换机
                "exchange.settlement.order",
                "key.order",
                null);
        rabbitAdmin.declareBinding(binding);

        /*---------reward---------*/
        //声明交换机
        exchange = new TopicExchange("exchange.order.reward");
        rabbitAdmin.declareExchange(exchange);

        //绑定队列
        binding = new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order",
                null);
        rabbitAdmin.declareBinding(binding);
    }
}
