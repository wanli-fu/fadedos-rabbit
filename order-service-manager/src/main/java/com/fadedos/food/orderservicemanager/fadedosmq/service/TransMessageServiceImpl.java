package com.fadedos.food.orderservicemanager.fadedosmq.service;

import com.fadedos.food.orderservicemanager.fadedosmq.dao.TransMessageDao;
import com.fadedos.food.orderservicemanager.fadedosmq.enummeration.TransMessageType;
import com.fadedos.food.orderservicemanager.fadedosmq.po.TransMessagePO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@Service
public class TransMessageServiceImpl implements TransMessageService {
    @Autowired
    private TransMessageDao transMessageDao;

    @Value("${fadedos.service}")
    String serviceName;

    @Override
    public TransMessagePO messageSendReady(String exchange, String routingKey, String body) {
        final String messageId = UUID.randomUUID().toString();
        TransMessagePO transMessagePO = new TransMessagePO();
        transMessagePO.setId(messageId);
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(exchange);
        transMessagePO.setRoutingKey(routingKey);
        transMessagePO.setPayload(body);
        transMessagePO.setSequence(0);
        transMessagePO.setType(TransMessageType.SEND);
        transMessagePO.setDate(new Date());

        //存库
        transMessageDao.insert(transMessagePO);
        return transMessagePO;
    }

    @Override
    public void messageSendSuccess(String id) {
        transMessageDao.delete(id, serviceName);
    }

    @Override
    public TransMessagePO messageSendReturn(String id, String exchange, String routingKey, String body) {
        return messageSendReady(exchange, routingKey, body);
    }

    @Override
    public List<TransMessagePO> listReadyMessages() {
        return transMessageDao.selectByTypeAndService(TransMessageType.SEND.toString(), serviceName);
    }

    @Override
    public void messageResend(String id) {
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);
        //这里不是一个原子操作  需要分布式锁 重发业务不要并行跑
        transMessagePO.setSequence(transMessagePO.getSequence() + 1);
        transMessageDao.update(transMessagePO);
    }

    @Override
    public void messageDead(String id) {
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);
        transMessagePO.setType(TransMessageType.DEAD);
        transMessageDao.update(transMessagePO);
    }

    @Override
    public void messageDead(String id, String exchange, String routingKey, String queue, String body) {
        TransMessagePO transMessagePO = new TransMessagePO();
        transMessagePO.setId(id);
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(exchange);
        transMessagePO.setRoutingKey(routingKey);
        transMessagePO.setQueue(queue);
        //死信状态
        transMessagePO.setType(TransMessageType.DEAD);
        transMessagePO.setPayload(body);
        transMessagePO.setDate(new Date());
        transMessagePO.setSequence(0);
    }

    @Override
    public TransMessagePO messageReceiveReady(String id, String exchange, String routingKey, String queue, String body) {
        //先查询数据库是否存在这条消息
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);


        if (null == transMessagePO) {
            transMessagePO = new TransMessagePO();
            transMessagePO.setId(id);
            transMessagePO.setService(serviceName);
            transMessagePO.setExchange(exchange);
            transMessagePO.setRoutingKey(routingKey);
            transMessagePO.setQueue(queue);
            transMessagePO.setType(TransMessageType.RECEIVE);
            transMessagePO.setPayload(body);
            transMessagePO.setDate(new Date());
            transMessagePO.setSequence(0);

            //存库
            transMessageDao.insert(transMessagePO);
        } else {
            transMessagePO.setSequence(transMessagePO.getSequence() + 1);
            transMessageDao.update(transMessagePO);
        }
        return transMessagePO;
    }

    @Override
    public void messageReceiveSuccess(String id) {
        transMessageDao.delete(id, serviceName);
    }
}
