package com.fadedos.deliverymanservicemanager.service;

import com.fadedos.deliverymanservicemanager.dao.DeliverymanDao;
import com.fadedos.deliverymanservicemanager.dto.OrderMessageDTO;
import com.fadedos.deliverymanservicemanager.enummeration.DeliverymanStatus;
import com.fadedos.deliverymanservicemanager.po.DeliverymanPO;
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
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {

    @Autowired
    DeliverymanDao deliverymanDao;

    ObjectMapper objectMapper = new ObjectMapper();

    DeliverCallback deliverCallback = (consumerTag, message) -> {

        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");


        try {
            //将消息序列化为DTO对象
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);

            //查询空闲骑手
            List<DeliverymanPO> deliverymanPOS = deliverymanDao.selectAvaliableDeliveryman(DeliverymanStatus.AVAILABLE);

            orderMessageDTO.setDeliverymanId(deliverymanPOS.get(0).getId());
            log.info("onMessage:restaurantOrderMessageDTO:{}", orderMessageDTO);

            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {

                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish(
                        "exchange.order.deliveryman",
                        "key.order",
                        null,
                        messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    };

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.deliveryman",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.deliveryman",
                    "exchange.order.deliveryman",
                    "key.deliveryman");


            channel.basicConsume("queue.deliveryman", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(100000);
            }
        }
    }
}

