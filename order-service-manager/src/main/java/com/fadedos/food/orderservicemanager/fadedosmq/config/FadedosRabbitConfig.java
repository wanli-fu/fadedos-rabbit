package com.fadedos.food.orderservicemanager.fadedosmq.config;

import com.fadedos.food.orderservicemanager.fadedosmq.service.TransMessageService;
import com.rabbitmq.client.AMQP;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Host;
import org.apache.catalina.Service;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@Configuration
@Slf4j
public class FadedosRabbitConfig {
    @Autowired
    private TransMessageService transMessageService;

    @Value("${fadedos.host}")
    private String host;

    @Value("${fadedos.port}")
    private int port;

    @Value("${fadedos.username}")
    private String username;

    @Value("${fadedos.password}")
    private String password;

    @Value("${fadedos.vhost}")
    private String vhost;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setVirtualHost(vhost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        //开启确认
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /**
     * 消息监听者 有消息就消费
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory listenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        listenerContainerFactory.setConnectionFactory(connectionFactory);
        listenerContainerFactory.setConcurrentConsumers(3);//建议写在配置文件
        listenerContainerFactory.setMaxConcurrentConsumers(10);
        //消息手动签收
        listenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return listenerContainerFactory;
    }

    @Bean
    public RabbitTemplate customerRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);

        //发送端 消息到达broker  交换机 回调此方法
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            log.info("RabbitConfig.rabbitTemplate.correlationData:{},ack:{},cause:{}", correlationData, ack, cause);

            if (ack && null !=correlationData){
                String messageId = correlationData.getId();
                log.info("FadedosRabbitConfig.customerRabbitTemplate,消息已经正确投递到交换机 messageId:{}", messageId);
                transMessageService.messageSendSuccess(messageId);
            } else {
                log.error("消息投递至交换机失败,correlationData;{}",correlationData);
            }
        });

        //发送端 到达broker 但是未到正确queue 就会回调此方法
        rabbitTemplate.setReturnsCallback(returned -> {
            //一定存在错误
            int replyCode = returned.getReplyCode();
            log.error("消息无法路由,错误代码为:replyCode"+replyCode);
            //除了打印log 还可以做别的业务逻辑

            //返回回调 路由失败重新持久化
            transMessageService.messageSendReturn(
                    returned.getMessage().getMessageProperties().getMessageId(),
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    new String(returned.getMessage().getBody())
            );

        });

        return rabbitTemplate;
    }
}
