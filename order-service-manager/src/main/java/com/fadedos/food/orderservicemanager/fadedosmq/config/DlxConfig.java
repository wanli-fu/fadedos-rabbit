package com.fadedos.food.orderservicemanager.fadedosmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/3/1
 */
@Configuration
@Slf4j
public class DlxConfig {

    /**
     * 死信交换机
     *
     * @return
     */
    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange("exchange.dlx");
    }

    /**
     * 死信交换机
     * @return
     */
    @Bean
    public Queue dlxQueue() {
        return new Queue(
                "queue.dlx",
                true,
                false,
                false);
    }

    /**
     * 绑定
     * @return
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("#");
    }
}
