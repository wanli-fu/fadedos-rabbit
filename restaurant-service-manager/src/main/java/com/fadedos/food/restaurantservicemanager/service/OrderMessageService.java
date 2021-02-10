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

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private ProductDao productDao;

    @Autowired
    private RestaurantDao restaurantDao;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {


            //声明 交换机
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            // 谁监听这个队列 谁声明 谁绑定
            //声明队列
            channel.queueDeclare(
                    "queue.restaurant",
                    true,
                    false,
                    false,
                    null);

            //绑定
            channel.queueBind(
                    "queue.restaurant",
                    "exchange.order.restaurant",
                    "key.restaurant"
            );

            //监听的队列
            channel.basicConsume(
                    "queue.restaurant",
                    true,
                    deliverCallback,
                    consumerTag -> {
                    });

            while (true) {
                Thread.sleep(100000);
            }
        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        //取出消息
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);


        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

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
            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                //序列化消息体 并发布
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish(
                        "exchange.order.restaurant",
                        "key.order",
                        null,
                        messageToSend.getBytes());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}

