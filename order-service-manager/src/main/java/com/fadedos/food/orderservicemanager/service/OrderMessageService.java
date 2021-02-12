package com.fadedos.food.orderservicemanager.service;


import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 消息处理相关业务逻辑
 *
 * @author wlzfw
 */
@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private OrderDetailDao orderDetailDao;
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 声明消息队列,交换机,绑定,消息处理
     */
    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            /*---------restaurant---------*/
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);
            channel.queueDeclare(
                    "queue.order",
                    true,
                    false,
                    false,
                    null
            );

            channel.queueBind(
                    "queue.order",
                    "exchange.order.restaurant",
                    "key.order"
            );

            /*---------deliveryman---------*/
            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.deliveryman",
                    "key.order"
            );

            /*---------settlement---------*/
            channel.exchangeDeclare(
                    "exchange.order.settlement",
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.settlement",
                    "key.order"
            );

            //消费消息
            channel.basicConsume("queue.order", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(10000000);
            }
        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        String messageBody = new String(message.getBody());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try {
            //消息体反序列化为DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);

            //数据库中读取订PO
            OrderDetailPO orderDetailPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());

            switch (orderDetailPO.getStatus()) {
                //商家回复消息
                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        //存库
                        orderDetailPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderDetailPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailDao.update(orderDetailPO);

                        //给骑手微服务发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.deliveryman",
                                    "key.deliveryman",//骑手微服务声明的routing key 谁接收消息谁申明routing key和队列
                                    null,
                                    messageToSend.getBytes()
                            );

                        }
                    } else {
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        //持久化订单
                        orderDetailPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderDetailPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailDao.update(orderDetailPO);

                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.settlement",
                                    "key.order",
                                    null,
                                    messageToSend.getBytes()
                            );
                        }
                    } else {
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    break;
                case SETTLEMENT_CONFIRMED:
                    break;
                case ORDER_CREATED:
                    break;
                case FAILED:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + orderDetailPO.getStatus());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}
