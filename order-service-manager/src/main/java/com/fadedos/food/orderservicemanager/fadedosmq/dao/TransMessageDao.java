package com.fadedos.food.orderservicemanager.fadedosmq.dao;

import com.fadedos.food.orderservicemanager.fadedosmq.po.TransMessagePO;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/28
 */
@Mapper
@Repository
public interface TransMessageDao {

    @Insert("INSERT INTO trans_message (id, type, service, " +
            "exchange, routingkey, queue, sequence, payload," +
            "date) " +
            "VALUES(#{id}, #{type}, #{service},#{exchange}," +
            "#{routingKey},#{queue},#{sequence}, #{payload},#{date})")
    void insert(TransMessagePO transMessagePO);

    @Update("UPDATE trans_message set type=#{type}, " +
            "service=#{service}, exchange =#{exchange},"+
            "routingkey =#{routingKey}, queue =#{queue}, " +
            "sequence =#{sequence}, payload =#{payload}, " +
            "date =#{date} " +
            "where id=#{id} and service=#{service}")
    void update(TransMessagePO transMessagePO);


    @Select("SELECT id, type, service, exchange, " +
            "routingkey routingKey, queue, sequence, " +
            "payload, date " +
            "FROM trans_message " +
            "where id=#{id} and service=#{service}")
    TransMessagePO selectByIdAndService(@Param("id") String id,
                                        @Param("service") String service);

    @Select("SELECT id, type, service, exchange, " +
            "routingkey routingKey, queue, sequence, " +
            "payload, date " +
            "FROM trans_message " +
            "WHERE type = #{type} and service = #{service}")
    List<TransMessagePO> selectByTypeAndService(
            @Param("type")String type,
            @Param("service")String service);


    @Delete("DELETE FROM trans_message " +
            "where id=#{id} and service=#{service}")
    void delete(@Param("id") String id,
                @Param("service") String service);
}
