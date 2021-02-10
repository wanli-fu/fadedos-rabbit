package com.fadedos.deliverymanservicemanager.dao;

import com.fadedos.deliverymanservicemanager.enummeration.DeliverymanStatus;
import com.fadedos.deliverymanservicemanager.po.DeliverymanPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface DeliverymanDao {

    @Select("SELECT id,name,status,date FROM deliveryman WHERE id = #{id}")
    DeliverymanPO selectDeliveryman(Integer id);

    /**
     * 根据状态查询外卖员
     * @param status
     * @return
     */
    @Select("SELECT id,name,status,date FROM deliveryman WHERE status = #{status}")
    List<DeliverymanPO> selectAvaliableDeliveryman(DeliverymanStatus status);
}
