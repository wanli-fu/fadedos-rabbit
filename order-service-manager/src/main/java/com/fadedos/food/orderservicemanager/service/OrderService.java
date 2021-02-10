package com.fadedos.food.orderservicemanager.service;


import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fadedos.food.orderservicemanager.vo.OrderCreateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Description:处理用户关于订单的业务请求
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderDetailDao orderDetailDao;

    /**
     * 使用jackson
     */
    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        log.info("createOder:orderCreateVO:{}", orderCreateVO);

        OrderDetailPO orderDetailPO = new OrderDetailPO();
        orderDetailPO.setAddress(orderCreateVO.getAddress());
        orderDetailPO.setAccountId(orderCreateVO.getAccountId());
        orderDetailPO.setProductId(orderCreateVO.getProductId());
        orderDetailPO.setStatus(OrderStatus.ORDER_CREATING);
        orderDetailPO.setDate(new Date());

        //持久化订单
        orderDetailDao.insert(orderDetailPO);

        //存库之后给下一个餐厅微服务发送消息
        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        //数据库中自动取出  mybatis
        orderMessageDTO.setOrderId(orderDetailPO.getId());
        orderMessageDTO.setProductId(orderDetailPO.getProductId());
        orderMessageDTO.setAccountId(orderDetailPO.getAccountId());

        //获取rabbit服务,并发布消息给商家
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("newpassword");

        try (Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel()) {
            //DTO转换为json
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            //发布消息
            channel.basicPublish(
                    "exchange.order.restaurant",
                    "key.restaurant",
                    null,
                    messageToSend.getBytes()
            );
        }
    }
}
