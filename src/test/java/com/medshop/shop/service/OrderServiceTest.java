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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试（M1，模块内逻辑最重）：地址归属、药品存在性、
 * 处方药拦截、库存校验、价格快照、并发扣减回滚、发货生成物流号、查询归属。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderMapper orderMapper;
    @Mock DrugMapper drugMapper;
    @Mock AddressMapper addressMapper;
    @Mock PaymentService paymentService;
    @Mock PrescriptionService prescriptionService;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderMapper, drugMapper, addressMapper,
                paymentService, prescriptionService);
    }

    private Drug drug(long id, String name, String price, int stock, int isRx) {
        Drug d = new Drug();
        d.setId(id);
        d.setName(name);
        d.setPrice(new BigDecimal(price));
        d.setStock(stock);
        d.setIsRx(isRx);
        return d;
    }

    private CreateOrderDTO order(long addressId, long drugId, int qty) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setAddressId(addressId);
        CreateOrderDTO.Item it = new CreateOrderDTO.Item();
        it.setDrugId(drugId);
        it.setQuantity(qty);
        dto.setItems(List.of(it));
        return dto;
    }

    // ---------- 创建订单：正常路径 ----------

    @Test
    @DisplayName("下单成功：OTC药品，金额=单价×数量，落待支付单，扣库存，生成支付记录")
    void createOrder_otc_success() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5001L)).thenReturn(drug(5001L, "感冒灵", "18.00", 100, 0));
        when(drugMapper.decreaseStock(5001L, 2)).thenReturn(1);
        // insertOrder 回填订单id
        doAnswer(inv -> { ((Order) inv.getArgument(0)).setId(2001L); return null; })
                .when(orderMapper).insertOrder(any());

        OrderDTO r = orderService.createOrder(1L, order(9001L, 5001L, 2));

        assertEquals(0, r.getStatus()); // 待支付
        assertEquals(0, new BigDecimal("36.00").compareTo(r.getTotalAmount()));
        verify(drugMapper).decreaseStock(5001L, 2);
        verify(paymentService).createPayment(eq(2001L), argThat(a -> a.compareTo(new BigDecimal("36.00")) == 0));
    }

    @Test
    @DisplayName("价格快照：下单明细记录的是下单时的单价")
    void createOrder_priceSnapshot() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5001L)).thenReturn(drug(5001L, "感冒灵", "18.00", 100, 0));
        when(drugMapper.decreaseStock(anyLong(), anyInt())).thenReturn(1);
        doAnswer(inv -> { ((Order) inv.getArgument(0)).setId(2001L); return null; })
                .when(orderMapper).insertOrder(any());

        orderService.createOrder(1L, order(9001L, 5001L, 3));

        ArgumentCaptor<List<OrderItem>> cap = ArgumentCaptor.forClass(List.class);
        verify(orderMapper).insertItems(cap.capture());
        OrderItem oi = cap.getValue().get(0);
        assertEquals(0, new BigDecimal("18.00").compareTo(oi.getUnitPrice()));
        assertEquals(3, oi.getQuantity());
        assertEquals(2001L, oi.getOrderId());
    }

    // ---------- 创建订单：校验失败路径 ----------

    @Test
    @DisplayName("下单：收货地址不属于当前用户则拦截，不落单")
    void createOrder_addressNotOwned_throws() {
        when(addressMapper.countByIdAndUser(9999L, 1L)).thenReturn(0);
        BizException ex = assertThrows(BizException.class,
                () -> orderService.createOrder(1L, order(9999L, 5001L, 1)));
        assertTrue(ex.getMessage().contains("收货地址"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("下单：药品不存在则拦截")
    void createOrder_drugNotFound_throws() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(8888L)).thenReturn(null);
        assertThrows(BizException.class, () -> orderService.createOrder(1L, order(9001L, 8888L, 1)));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("下单：处方药无审核通过处方则拦截并引导上传")
    void createOrder_rxWithoutApproval_throws() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5004L)).thenReturn(drug(5004L, "阿莫西林", "25.00", 50, 1));
        when(prescriptionService.hasApproved(1L, 5004L)).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> orderService.createOrder(1L, order(9001L, 5004L, 1)));
        assertTrue(ex.getMessage().contains("处方药"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("下单：处方药已通过审核则放行")
    void createOrder_rxApproved_success() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5004L)).thenReturn(drug(5004L, "阿莫西林", "25.00", 50, 1));
        when(prescriptionService.hasApproved(1L, 5004L)).thenReturn(true);
        when(drugMapper.decreaseStock(5004L, 1)).thenReturn(1);
        doAnswer(inv -> { ((Order) inv.getArgument(0)).setId(3001L); return null; })
                .when(orderMapper).insertOrder(any());

        OrderDTO r = orderService.createOrder(1L, order(9001L, 5004L, 1));
        assertEquals(0, new BigDecimal("25.00").compareTo(r.getTotalAmount()));
    }

    @Test
    @DisplayName("下单：库存不足（下单时即不够）则拦截")
    void createOrder_insufficientStock_throws() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5001L)).thenReturn(drug(5001L, "感冒灵", "18.00", 1, 0));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.createOrder(1L, order(9001L, 5001L, 5)));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("下单：并发抢购致扣减影响行数为0，抛异常触发事务回滚")
    void createOrder_concurrentSoldOut_throws() {
        when(addressMapper.countByIdAndUser(9001L, 1L)).thenReturn(1);
        when(drugMapper.selectById(5001L)).thenReturn(drug(5001L, "感冒灵", "18.00", 100, 0));
        when(drugMapper.decreaseStock(5001L, 2)).thenReturn(0); // 被并发抢空
        doAnswer(inv -> { ((Order) inv.getArgument(0)).setId(2001L); return null; })
                .when(orderMapper).insertOrder(any());

        BizException ex = assertThrows(BizException.class,
                () -> orderService.createOrder(1L, order(9001L, 5001L, 2)));
        assertTrue(ex.getMessage().contains("并发"));
        // 已未生成支付（异常在扣库存阶段抛出，事务回滚由 @Transactional 负责）
        verify(paymentService, never()).createPayment(anyLong(), any());
    }

    // ---------- 查询 ----------

    @Test
    @DisplayName("查订单详情：归属本人，返回含明细")
    void getOrderDetail_success() {
        Order o = new Order();
        o.setId(2001L);
        o.setUserId(1L);
        o.setTotalAmount(new BigDecimal("36.00"));
        o.setStatus(1);
        when(orderMapper.selectById(2001L)).thenReturn(o);
        when(orderMapper.selectItemsByOrderId(2001L)).thenReturn(List.of(new OrderItem()));

        OrderDTO dto = orderService.getOrderDetail(1L, 2001L);
        assertEquals(2001L, dto.getOrderId());
        assertEquals(1, dto.getItems().size());
    }

    @Test
    @DisplayName("查订单详情：非本人订单抛「无权查看」")
    void getOrderDetail_notOwned_throws() {
        Order o = new Order();
        o.setId(2001L);
        o.setUserId(999L);
        when(orderMapper.selectById(2001L)).thenReturn(o);

        assertThrows(BizException.class, () -> orderService.getOrderDetail(1L, 2001L));
    }

    @Test
    @DisplayName("查订单详情：订单不存在抛异常")
    void getOrderDetail_notFound_throws() {
        when(orderMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> orderService.getOrderDetail(1L, 2001L));
    }

    // ---------- 更新状态 / 发货 ----------

    @Test
    @DisplayName("发货(置为配送中2)：自动生成SF开头物流单号")
    void updateStatus_ship_generatesLogisticsNo() {
        Order o = new Order();
        o.setId(2001L);
        o.setStatus(1);
        o.setLogisticsNo(null);
        when(orderMapper.selectById(2001L)).thenReturn(o);

        orderService.updateStatus(2001L, 2);

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(orderMapper).updateStatus(eq(2001L), eq(2), cap.capture());
        assertNotNull(cap.getValue());
        assertTrue(cap.getValue().startsWith("SF"));
    }

    @Test
    @DisplayName("更新状态：已有物流号时发货不覆盖原单号")
    void updateStatus_ship_keepsExistingNo() {
        Order o = new Order();
        o.setId(2001L);
        o.setStatus(2);
        o.setLogisticsNo("SF0000000001");
        when(orderMapper.selectById(2001L)).thenReturn(o);

        orderService.updateStatus(2001L, 2);
        verify(orderMapper).updateStatus(2001L, 2, "SF0000000001");
    }

    @Test
    @DisplayName("更新状态：非发货状态不生成物流号")
    void updateStatus_nonShip_noLogistics() {
        Order o = new Order();
        o.setId(2001L);
        o.setStatus(0);
        when(orderMapper.selectById(2001L)).thenReturn(o);

        orderService.updateStatus(2001L, 1); // 已支付
        verify(orderMapper).updateStatus(2001L, 1, null);
    }

    @Test
    @DisplayName("更新状态：订单不存在抛异常")
    void updateStatus_notFound_throws() {
        when(orderMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> orderService.updateStatus(2001L, 2));
    }

    @Test
    @DisplayName("更新状态：非法状态值(<0或>4)抛异常")
    void updateStatus_illegalStatus_throws() {
        Order o = new Order();
        o.setId(2001L);
        o.setStatus(1);
        when(orderMapper.selectById(2001L)).thenReturn(o);

        assertThrows(BizException.class, () -> orderService.updateStatus(2001L, -1));
        assertThrows(BizException.class, () -> orderService.updateStatus(2001L, 5));
        verify(orderMapper, never()).updateStatus(anyLong(), anyInt(), any());
    }
}
