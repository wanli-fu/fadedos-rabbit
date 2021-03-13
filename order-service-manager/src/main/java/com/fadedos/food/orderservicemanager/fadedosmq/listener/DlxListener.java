package com.fadedos.food.orderservicemanager.fadedosmq.listener;

import com.fadedos.food.orderservicemanager.fadedosmq.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/3/1
 */
@Component
@Slf4j
@ConditionalOnProperty("fadedos.dlxEnabled")
public class DlxListener implements ChannelAwareMessageListener {

    @Autowired
    private TransMessageService transMessageService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        log.error("dead letter! message:{}", message);


        //发邮件 打电话  发短信
        //XXXX()

        MessageProperties messageProperties = message.getMessageProperties();
        transMessageService.messageDead(
                messageProperties.getMessageId(),
                messageProperties.getReceivedExchange(),
                messageProperties.getReceivedRoutingKey(),
                messageProperties.getConsumerQueue(),
                messageBody);

        channel.basicAck(messageProperties.getDeliveryTag(),false);
    }
}
