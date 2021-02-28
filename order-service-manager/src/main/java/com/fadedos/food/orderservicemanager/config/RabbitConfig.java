package com.fadedos.food.orderservicemanager.config;

import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.service.OrderMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Configuration
@Slf4j
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;

//    @Autowired //该方法会被自动在spring boot启动时调用
//    public void startListenMessage() throws InterruptedException, TimeoutException, IOException {
//        orderMessageService.handleMessage();
//    }

//    /*---------restaurant---------*/
//    @Bean
//    public Exchange exchange1() {
//        return new DirectExchange("exchange.order.restaurant");
//    }
//
//    @Bean
//    public Queue queue1() {
//        return new Queue("queue.order");
//    }
//
//    @Bean
//    public Binding binding1() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.restaurant",
//                "key.order",
//                null);
//    }
//
//    /*---------deliveryman---------*/
//    @Bean
//    public Exchange exchange2() {
//        return new DirectExchange("exchange.order.deliveryman");
//    }
//
//
//    @Bean
//    public Binding binding2() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.deliveryman",
//                "key.order",
//                null);
//    }
//
//    /*---------settlement---------*/
//    @Bean
//    public Exchange exchange3() {
//        return new FanoutExchange("exchange.order.settlement");
//    }
//
//    @Bean
//    public Exchange exchange4() {
//        return new FanoutExchange("exchange.settlement.order");
//    }
//
//    @Bean
//    public Binding binding3() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                //由于使用的fanout 群发 则须用两个虚拟机  订单模块 接收的到 结算模块的消息 是这个交换机
//                "exchange.settlement.order",
//                "key.order",
//                null);
//    }
//
//    /*---------reward---------*/
//    @Bean
//    public Exchange exchange5() {
//        return new TopicExchange("exchange.order.reward");
//    }
//
//
//    @Bean
//    public Binding binding4() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.reward",
//                "key.order",
//                null);
//    }

//    @Bean
//    public ConnectionFactory connectionFactory() {
//        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
//        connectionFactory.setHost("129.28.198.9");
//        connectionFactory.setPort(5672);
//        connectionFactory.setUsername("guest");
//        connectionFactory.setPassword("newpassword");
//        //开启确认
//        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
//        connectionFactory.setPublisherReturns(true);
//        connectionFactory.createConnection();
//        return connectionFactory;
//    }
//
//    @Bean
//    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
//        rabbitAdmin.setAutoStartup(true);
//        return rabbitAdmin;
//
//    }
//
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        rabbitTemplate.setMandatory(true);
//
//        //发送端 未投递到 queue 退回模式 就会回调
//        rabbitTemplate.setReturnsCallback(returned -> {
//            int replyCode = returned.getReplyCode();
//            log.info("replyCode" + String.valueOf(replyCode));
//            //除了打印log,还可以加别的业务逻辑
//        });
//
//        //发送端确认 消息到达broker 就会回调
//        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
//            log.info("RabbitConfig.rabbitTemplate.correlationData:{},ack:{},cause:{}", correlationData, ack, cause);
//
//        });
//        return rabbitTemplate;
//    }
//
//    @Bean
//    public RabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
//        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//        factory.setConnectionFactory(connectionFactory);
//        return factory;
//    }

//    @Bean
//    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory) {
//        SimpleMessageListenerContainer simpleMessageListenerContainer =
//                new SimpleMessageListenerContainer(connectionFactory);
//        simpleMessageListenerContainer.setQueueNames("queue.order");
//        //相当于前面线程池的大小 有几个消费者
//        simpleMessageListenerContainer.setConcurrentConsumers(3);
//        simpleMessageListenerContainer.setMaxConcurrentConsumers(5);
////        simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
////
////        simpleMessageListenerContainer.setMessageListener(message -> {
////            log.info("message:{}", message);
////            // 此处写消息处理接收的的逻辑,相当于    DeliverCallback deliverCallback = ((consumerTag, message) -> {
////        });
//
//        //消息手动确认
//        simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//
////        //不优雅
////        simpleMessageListenerContainer.setMessageListener(new ChannelAwareMessageListener() {
////            @Override
////            public void onMessage(Message message, Channel channel) throws Exception {
////                log.info("message:{}", message);
////                // 此处写消息处理接收的的逻辑,相当于    DeliverCallback deliverCallback = ((consumerTag, message) -> {
////                orderMessageService.handleMessage(message.getBody());
////                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
////            }
////        });
//
//        //消费端限流
//        simpleMessageListenerContainer.setPrefetchCount(1);
//
//        //Message
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(orderMessageService);
//        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter();
//        messageConverter.setClassMapper(new ClassMapper() {
//            @Override
//            public void fromClass(Class<?> aClass, MessageProperties messageProperties) {
//
//            }
//
//            @Override
//            public Class<?> toClass(MessageProperties messageProperties) {
//                return OrderMessageDTO.class;
//            }
//        });
//
//        //1条 8 10条 16 100条 256 一般是0.75倍
//        Map<String, String> methodMap = new HashMap<>(8);
//        //key 为队列名 value为 处理消息的方法
//        methodMap.put("queue.order", "handleMessag");
//        //可以自动根据不同队列 进行不同方法的处理
//        methodMap.put("queue.order1", "handleMessag1");
//        messageListenerAdapter.setQueueOrTagToMethodName(methodMap);
//        //消息转换
////        Jackson2JavaTypeMapper jackson2JavaTypeMapper = new DefaultJackson2JavaTypeMapper();不常用
////        messageConverter.setJavaTypeMapper(jackson2JavaTypeMapper);
//        messageListenerAdapter.setMessageConverter(messageConverter);
//
//        simpleMessageListenerContainer.setMessageListener(messageListenerAdapter);
//        return simpleMessageListenerContainer;
//    }
}
