package com.fadedos.food.orderservicemanager.fadedosmq.po;

import com.fadedos.food.orderservicemanager.fadedosmq.enummeration.TransMessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@Getter
@Setter
@ToString
public class TransMessagePO {
    //此处消息id 用的uuid
    private String id;
    //服务名字 和id 是数据库中联合主键
    private String service;
    //消息类型
    private TransMessageType type;
    //消息有关的元数据 交换机
    private String exchange;
    //绑定键
    private String routingKey;
    //队列
    private String queue;
    //序号 消息第几次发送
    private Integer sequence;
    //消息内容
    private String payload;
    private Date date;
}
