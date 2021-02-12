package com.fadedos.food.settlementservicemanager.service;

import com.fadedos.food.settlementservicemanager.dao.SettlementDao;
import com.fadedos.food.settlementservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.settlementservicemanager.enummeration.SettlementStatus;
import com.fadedos.food.settlementservicemanager.po.SettlementPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/12
 */
@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementDao settlementDao;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start listener message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(
                    "exchange.settlement.order",
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.settlement",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.settlement",
                    "exchange.order.settlement",
                    "key.settlement",
                    null);

            channel.basicConsume(
                    "queue.settlement",
                    true,
                    deliverCallback, consumerTag -> {
                    });

            while (true){
                Thread.sleep(100000);
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
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);

            SettlementPO settlementPO = new SettlementPO();
            settlementPO.setOrderId(orderMessageDTO.getOrderId());
            settlementPO.setAmount(orderMessageDTO.getPrice());
            settlementPO.setDate(new Date());
            Integer settlementId = settlementService.settlement(
                    orderMessageDTO.getAccountId(),
                    orderMessageDTO.getPrice());

            settlementPO.setStatus(SettlementStatus.SUCCESS);
            settlementPO.setTransactionId(settlementId);

            //结算模块 存库
            settlementDao.insert(settlementPO);

            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                //序列化消息体 并发布
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish(
                        "exchange.settlement.order",
                        "key.order",
                        null,
                        messageToSend.getBytes());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}
