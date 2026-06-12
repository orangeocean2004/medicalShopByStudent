package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.shop.dto.CreateOrderDTO;
import com.medshop.shop.dto.OrderDTO;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.entity.Order;
import com.medshop.shop.entity.OrderItem;
import com.medshop.shop.mapper.AddressMapper;
import com.medshop.shop.mapper.DrugMapper;
import com.medshop.shop.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务层（M1，模块内逻辑最重的类）：创建订单、查询订单与物流。
 *
 * <p>{@link #createOrder} 严格落实《代码设计》要点：地址归属校验、处方药拦截、
 * 库存校验、价格快照、扣减库存、生成待支付记录，全程事务保证一致性。
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final DrugMapper drugMapper;
    private final AddressMapper addressMapper;
    private final PaymentService paymentService;
    private final PrescriptionService prescriptionService;

    public OrderService(OrderMapper orderMapper, DrugMapper drugMapper,
                        AddressMapper addressMapper, PaymentService paymentService,
                        PrescriptionService prescriptionService) {
        this.orderMapper = orderMapper;
        this.drugMapper = drugMapper;
        this.addressMapper = addressMapper;
        this.paymentService = paymentService;
        this.prescriptionService = prescriptionService;
    }

    /**
     * 创建订单（FP-SHOP-04）。事务内完成校验、扣库存、落单、生成待支付记录。
     */
    @Transactional
    public OrderDTO createOrder(Long userId, CreateOrderDTO dto) {
        // 1. 校验收货地址归属当前用户
        if (addressMapper.countByIdAndUser(dto.getAddressId(), userId) == 0) {
            throw new BizException("收货地址不存在或不属于当前用户");
        }

        // 2. 遍历下单项：取药品、处方药拦截、库存校验、累加金额、构建明细（价格快照）
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderDTO.Item item : dto.getItems()) {
            Drug drug = drugMapper.selectById(item.getDrugId());
            if (drug == null) {
                throw new BizException("药品不存在：" + item.getDrugId());
            }
            // 处方药需有该药「审核通过」的处方，否则拦截并引导上传（M4）
            if (drug.getIsRx() != null && drug.getIsRx() == 1) {
                if (!prescriptionService.hasApproved(userId, drug.getId())) {
                    throw new BizException("「" + drug.getName() + "」为处方药，请先上传处方并通过药师审核后再购买");
                }
            }
            if (drug.getStock() < item.getQuantity()) {
                throw new BizException("「" + drug.getName() + "」库存不足，当前库存 " + drug.getStock());
            }
            totalAmount = totalAmount.add(drug.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            OrderItem oi = new OrderItem();
            oi.setDrugId(drug.getId());
            oi.setQuantity(item.getQuantity());
            oi.setUnitPrice(drug.getPrice());   // 价格快照，防止药品改价污染历史订单
            items.add(oi);
        }

        // 3. 插入订单（待支付），回填 orderId
        Order order = new Order();
        order.setUserId(userId);
        order.setAddressId(dto.getAddressId());
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        orderMapper.insertOrder(order);

        // 4. 批量插入明细
        for (OrderItem oi : items) {
            oi.setOrderId(order.getId());
        }
        orderMapper.insertItems(items);

        // 5. 扣减库存（WHERE stock>=quantity，受影响行数为 0 说明并发下被抢空）
        for (OrderItem oi : items) {
            int affected = drugMapper.decreaseStock(oi.getDrugId(), oi.getQuantity());
            if (affected == 0) {
                throw new BizException("下单失败：药品库存不足（并发抢购）"); // 触发事务回滚
            }
        }

        // 6. 生成待支付记录（与下单解耦）
        paymentService.createPayment(order.getId(), totalAmount);

        // 7. 组装返回
        OrderDTO result = new OrderDTO();
        result.setOrderId(order.getId());
        result.setTotalAmount(totalAmount);
        result.setStatus(0);
        return result;
    }

    /**
     * 查询订单与物流进度（FP-SHOP-05）。校验归属，返回含明细的订单。
     */
    public OrderDTO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在或无权查看");
        }
        List<OrderItem> items = orderMapper.selectItemsByOrderId(orderId);

        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setLogisticsNo(order.getLogisticsNo());
        dto.setCreatedAt(fmt(order.getCreatedAt()));
        dto.setItems(items);
        return dto;
    }

    /** 我的订单列表（不含明细，按时间倒序）。 */
    public List<OrderDTO> listMyOrders(Long userId) {
        return toBriefList(orderMapper.selectByUser(userId));
    }

    /** 全部订单列表（运营后台，不含明细）。 */
    public List<OrderDTO> listAllOrders() {
        return toBriefList(orderMapper.selectAll());
    }

    /**
     * 更新订单状态（运营后台发货/完成）。状态流转：
     * 0待支付 → 1已支付 → 2配送中 → 3已完成 / 4已取消。
     * 置为「配送中」时自动生成物流单号。
     */
    public void updateStatus(Long orderId, Integer status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (status < 0 || status > 4) {
            throw new BizException("非法的订单状态");
        }
        String logisticsNo = order.getLogisticsNo();
        if (status == 2 && (logisticsNo == null || logisticsNo.isBlank())) {
            // 发货时生成一个演示物流单号
            logisticsNo = "SF" + String.format("%010d", order.getId() * 7 + 1000000);
        }
        orderMapper.updateStatus(orderId, status, logisticsNo);
    }

    /** 把订单实体列表转为简要 DTO 列表（不查明细，避免 N+1）。 */
    private List<OrderDTO> toBriefList(List<Order> orders) {
        List<OrderDTO> list = new ArrayList<>();
        for (Order o : orders) {
            OrderDTO dto = new OrderDTO();
            dto.setOrderId(o.getId());
            dto.setUserId(o.getUserId());
            dto.setTotalAmount(o.getTotalAmount());
            dto.setStatus(o.getStatus());
            dto.setLogisticsNo(o.getLogisticsNo());
            dto.setCreatedAt(fmt(o.getCreatedAt()));
            list.add(dto);
        }
        return list;
    }

    private String fmt(java.time.LocalDateTime t) {
        return t == null ? null : t.format(DATE_FMT);
    }

    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
}
