package com.fadedos.food.restaurantservicemanager.service;

import com.fadedos.food.restaurantservicemanager.dao.ProductDao;
import com.fadedos.food.restaurantservicemanager.dao.RestaurantDao;
import com.fadedos.food.restaurantservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.restaurantservicemanager.enummeration.ProductStatus;
import com.fadedos.food.restaurantservicemanager.enummeration.RestaurantStatus;
import com.fadedos.food.restaurantservicemanager.po.ProductPO;
import com.fadedos.food.restaurantservicemanager.po.RestaurantPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private ProductDao productDao;

    @Autowired
    private RestaurantDao restaurantDao;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Channel channel;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("OrderMessageService.handleMessage" + "start listening message");

        //声明死信交换机
        channel.exchangeDeclare(
                "exchange.dlx",
                BuiltinExchangeType.TOPIC,
                true,
                false,
                null);

        //声明接收死信的队列(注意它不是死信队列)
        channel.queueDeclare(
                "queue.dlx",
                true,
                false,
                false,
                null);

        //绑定
        channel.queueBind(
                "queue.dlx",
                "exchange.dlx",
                "#");

        //声明 交换机
        channel.exchangeDeclare(
                "exchange.order.restaurant",
                BuiltinExchangeType.DIRECT,
                true,
                false,
                null);

        Map<String, Object> args = new HashMap<>(16);
        //该队列中所有的消息过期时间
        args.put("x-message-ttl", 15000);
        args.put("x-message-ttl", 1500000);//测试死信 避免测试影响
//        args.put("x-expire", 15000); //不建议使用  这是队列的过期时间,会将队列删掉 导致路由失败

        //声明死信队列参数  此时加入这个参数的队列是死信队列
        args.put("x-dead-letter-exchange", "exchange.dlx");

        //设置队列的最大长度 最大长度为5
        args.put("x-max-length", 5);

        // 谁监听这个队列 谁声明 谁绑定
        //声明队列
        channel.queueDeclare(
                "queue.restaurant",
                true,
                false,
                false,
                args);

        //绑定
        channel.queueBind(
                "queue.restaurant",
                "exchange.order.restaurant",
                "key.restaurant"
        );

        //消费端限流Qos
        channel.basicQos(2);

        //监听的队列
        channel.basicConsume(
                "queue.restaurant",
                false,
                deliverCallback,
                consumerTag -> {
                });

        while (true) {
            Thread.sleep(100000);
        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        //取出消息
        String messageBody1 = new String(message.getBody()).replace("\\", "");
        String messageBody = messageBody1.substring(1, messageBody1.length() - 1);
        log.info("deliverCallback:messageBody:{}", messageBody);

        try {
            //消息反序列化为DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);

            //数据库中 商店和产品是否可用
            ProductPO productPO = productDao.selsctProduct(orderMessageDTO.getProductId());
            RestaurantPO restaurantPO = restaurantDao.selsctRestaurant(productPO.getRestaurantId());

            if (productPO.getStatus() == ProductStatus.AVAILABLE &&
                    restaurantPO.getStatus() == RestaurantStatus.OPEN) {
                //状态均ok
                orderMessageDTO.setConfirmed(true);
                orderMessageDTO.setPrice(productPO.getPrice());
            } else {
                orderMessageDTO.setConfirmed(false);
            }


//                channel.addReturnListener(new ReturnListener() {
////                    @Override
////                    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
////                        log.info("Message Return:"+
////                                "replyCode:{},replyText:{},exchange:{},routingKey:{},properties:{},body:{}",
////                                replyCode,replyText,exchange,routingKey,properties,new String(body));
////                        //除了打印log  可以加别的业务操作
////                    }
////                });

            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    int replyCode = returnMessage.getReplyCode();
                    //除了打印log  可以加别的业务操作
                }
            });
            Thread.sleep(3000);
            //手动签收
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            //nack 测试死信
//            channel.basicNack(message.getEnvelope().getDeliveryTag(), false,false);
            //序列化消息体 并发布
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish(
                    "exchange.order.restaurant",
                    "key.order",
                    true,
                    null,
                    messageToSend.getBytes());
            //避免channel不会关闭
            Thread.sleep(1000);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}

