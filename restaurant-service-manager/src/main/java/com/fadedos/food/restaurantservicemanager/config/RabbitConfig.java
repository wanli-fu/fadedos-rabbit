package com.fadedos.food.restaurantservicemanager.config;

import com.fadedos.food.restaurantservicemanager.service.OrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Description: service中 监听队列方法自动启动起来
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Configuration
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;

    @Autowired //该方法会被自动在spring boot启动时调用
    public void startListenMessage() throws InterruptedException, TimeoutException, IOException {
        orderMessageService.handleMessage();
    }
}
