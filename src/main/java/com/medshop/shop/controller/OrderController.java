package com.medshop.shop.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.shop.dto.CreateOrderDTO;
import com.medshop.shop.dto.OrderDTO;
import com.medshop.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制层（M1）。
 * <ul>
 *   <li>API-04 创建订单：POST /api/v1/orders</li>
 *   <li>API-05 查询订单与物流：GET /api/v1/orders/{id}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderDTO> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = UserContext.requireUserId();
        return Result.success(orderService.createOrder(userId, dto));
    }

    @GetMapping("/{id}")
    public Result<OrderDTO> getOrderDetail(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        return Result.success(orderService.getOrderDetail(userId, id));
    }

    /** 我的订单列表。 */
    @GetMapping
    public Result<java.util.List<OrderDTO>> myOrders() {
        Long userId = UserContext.requireUserId();
        return Result.success(orderService.listMyOrders(userId));
    }
}
