package com.fadedos.food.orderservicemanager.fadedosmq.task;

import com.fadedos.food.orderservicemanager.fadedosmq.po.TransMessagePO;
import com.fadedos.food.orderservicemanager.fadedosmq.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@EnableScheduling
@Configuration
@Slf4j
public class ResendTask {

    @Autowired
    private TransMessageService transMessageService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${fadedos.resendTimes}")
    private Integer resendTimes;

    @Scheduled(fixedDelayString = "${fadedos.resendFreq}")
    public void resendMessage() {
        log.info("ResendTask.resendMessage invoked");

        List<TransMessagePO> messagePOS = transMessageService.listReadyMessages();
        log.info("ResendTask.resendMessage.messagePOS:{}", messagePOS);

        for (TransMessagePO messagePO : messagePOS) {
            log.info("ResendTask.resendMessage.messagePO:{}", messagePO);
            if (messagePO.getSequence() >resendTimes){
                log.error("resend too many times!");
                transMessageService.messageDead(messagePO.getId());
                continue;
            }

            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            messageProperties.setMessageId(messagePO.getId());

            Message message = new Message(messagePO.getPayload().getBytes(),messageProperties);

            rabbitTemplate.convertAndSend(
                    messagePO.getExchange(),
                    messagePO.getRoutingKey(),
                    message,
                    new CorrelationData(messagePO.getId()));

            log.info("ResendTask.resendMessage.ID:{}", messagePO.getId());

            //重发次数 记录
            transMessageService.messageResend(messagePO.getId());


        }
    }
}
