package com.fadedos.food.orderservicemanager.fadedosmq.listener;

import com.fadedos.food.orderservicemanager.fadedosmq.po.TransMessagePO;
import com.fadedos.food.orderservicemanager.fadedosmq.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * 相当于@RabbitListener注解
 * @Description:
 * @author: pengcheng
 * @date: 2021/3/1
 */
@Slf4j
public abstract class AbstractMessageListener implements ChannelAwareMessageListener {

    @Autowired
    private TransMessageService transMessageService;

    public abstract void receiveMessage(Message message) throws IOException;

    /**
     * 重试次数
     */
    @Value("${fadedos.resendTimes}")
    private Integer resendTimes;

    @Override
    public void onMessage(Message message, Channel channel) throws IOException, InterruptedException {
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();

        //消息消费前 先持久化
        TransMessagePO transMessagePO = transMessageService.messageReceiveReady(
                messageProperties.getMessageId(),
                messageProperties.getReceivedExchange(),
                messageProperties.getReceivedRoutingKey(),
                messageProperties.getConsumerQueue(),
                new String(message.getBody()));
        log.info("收到消息id:{},消费重试次数:{}", transMessagePO.getId(), transMessagePO.getSequence());

        try {
            receiveMessage(message);
            channel.basicAck(deliveryTag, false);
            //消息成功消费
            transMessageService.messageSendSuccess(messageProperties.getMessageId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            if (transMessagePO.getSequence() >= resendTimes) {
                //重复消费次数太多
                channel.basicReject(deliveryTag, false);
            } else {
                // 延时重回队列
                Thread.sleep((long) (Math.pow(2, transMessagePO.getSequence()) * 1000));
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}