package com.fadedos.food.orderservicemanager.controller;

import com.fadedos.food.orderservicemanager.service.OrderService;
import com.fadedos.food.orderservicemanager.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Slf4j
@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    public void CreateOrder(@RequestBody OrderCreateVO orderCreateVO) throws IOException, TimeoutException, InterruptedException {
        //推荐使用占位符
        log.info("createOrder:orderCreateVo:{}", orderCreateVO);
        //不推荐  若改成error级别 就会出现 但是此时不应该出现
//        log.info("createOrder:orderCreateVo:"+orderCreateVO.toString());
        orderService.createOrder(orderCreateVO);
    }
}
