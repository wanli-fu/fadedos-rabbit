package com.fadedos.food.orderservicemanager.fadedosmq.sender;

import com.fadedos.food.orderservicemanager.fadedosmq.po.TransMessagePO;
import com.fadedos.food.orderservicemanager.fadedosmq.service.TransMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@Component
@Slf4j
public class TransMessageSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransMessageService transMessageService;

    public void send(String exchange, String routingKey, Object payLoad) {
        log.info("TransMessageSender.send.exchange:{} routingKey:{} payLoad:{}", exchange, routingKey, payLoad);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String payLoadStr = mapper.writeValueAsString(payLoad);

            TransMessagePO transMessagePO = transMessageService.messageSendReady(
                    exchange,
                    routingKey,
                    payLoadStr
            );

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            messageProperties.setMessageId(transMessagePO.getId());

            Message message = new Message(payLoadStr.getBytes(),messageProperties);

            rabbitTemplate.convertAndSend(exchange,routingKey,message,new CorrelationData(transMessagePO.getId()));

            log.info("TransMessageSender.send.ID:{}", transMessagePO.getId());


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
