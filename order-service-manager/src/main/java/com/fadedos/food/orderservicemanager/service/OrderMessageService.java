package com.fadedos.food.orderservicemanager.service;


import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sun.org.apache.bcel.internal.generic.IFNULL;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
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
//@RabbitListener(containerFactory = "rabbitListenerContainerFactory", queues = "queue.order")  放在类上 和方法上注解配合使用 @RabbitHandler(isDefault = true)
public class OrderMessageService {
    @Autowired
    private OrderDetailDao orderDetailDao;
    ObjectMapper objectMapper = new ObjectMapper();

//    /**
//     * 声明消息队列,交换机,绑定,消息处理
//     */
//    @Async
//    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
//        Thread.sleep(5000);
//        ConnectionFactory connectionFactory = new ConnectionFactory();
//        connectionFactory.setHost("129.28.198.9");
//        connectionFactory.setPort(5672);
//        connectionFactory.setUsername("guest");
//        connectionFactory.setPassword("newpassword");
//
//        try (Connection connection = connectionFactory.newConnection();
//             Channel channel = connection.createChannel()) {
//
//
//            //消费消息
////            channel.basicConsume("queue.order", true, deliverCallback, consumerTag -> {
////            });
//            while (true) {
//                Thread.sleep(10000000);
//            }
//        }
//    }

    //    @RabbitHandler(isDefault = true) 和放在类上配合使用
    @RabbitListener(
//            containerFactory = "rabbitListenerContainerFactory",
//            admin = "rabbitAdmin",
            bindings = {
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order"
//                                    arguments = {
//                                            @Argument(
//                                                    name = "x-message-ttl",
//                                                    value = "1000",
//                                                    type = "java.lang.Integer"
//                                            ),
//                                            @Argument(
//                                                    name = "x-dead-letter-exchange",
//                                                    value = "exchange.dlx"
//                                            )
//                                    }
                            ),
                            exchange = @Exchange(name = "exchange.order.restaurant"),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.order.deliveryman", type = ExchangeTypes.DIRECT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.settlement.order", type = ExchangeTypes.FANOUT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.order.reward", type = ExchangeTypes.TOPIC),
                            key = "key.order"
                    )
            }
    )
    public void handleMessag(@Payload Message message) {
        log.info("OrderMessageService.handleMessag.message:{}", new String(message.getBody()));


        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try {
            //消息体反序列化为DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);

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
                        //持久化订单  此处有骑手id 说明订单状态为骑手确认OK
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
                    if (null != orderMessageDTO.getSettlementId()) {
                        //存库
                        orderDetailPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderDetailPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailDao.update(orderDetailPO);

                        //给积分微服务发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.reward",
                                    "key.reward",
                                    null,
                                    messageToSend.getBytes()
                            );

                        }
                    } else {
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()) {
                        orderDetailPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderDetailPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderDetailPO);
                    } else {
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
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
    }
}
