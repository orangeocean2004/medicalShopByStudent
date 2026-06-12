package com.medshop.shop.mapper;

import com.medshop.shop.entity.Order;
import com.medshop.shop.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单与明细数据访问层。SQL 见 mapper/OrderMapper.xml。
 */
@Mapper
public interface OrderMapper {

    int insertOrder(Order order);

    /** 批量插入订单明细。 */
    int insertItems(@Param("items") List<OrderItem> items);

    Order selectById(@Param("id") Long id);

    /** 当前用户的订单列表（倒序，不含明细）。 */
    List<Order> selectByUser(@Param("userId") Long userId);

    /** 全部订单列表（运营后台用，倒序）。 */
    List<Order> selectAll();

    /** 更新订单状态与物流单号（运营后台发货/完成用）。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("logisticsNo") String logisticsNo);

    /** 查订单明细（联表带出药品名）。 */
    List<OrderItem> selectItemsByOrderId(@Param("orderId") Long orderId);
}
