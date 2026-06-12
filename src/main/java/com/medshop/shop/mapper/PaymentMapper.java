package com.medshop.shop.mapper;

import com.medshop.shop.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录数据访问层。SQL 见 mapper/PaymentMapper.xml。
 */
@Mapper
public interface PaymentMapper {

    /** 下单时生成待支付记录。 */
    int insert(Payment payment);
}
