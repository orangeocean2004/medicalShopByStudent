package com.medshop.shop.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.shop.dto.AdminStatsDTO;
import com.medshop.shop.dto.OrderDTO;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.mapper.DrugMapper;
import com.medshop.shop.service.OrderService;
import com.medshop.user.dto.UserProfileDTO;
import com.medshop.user.mapper.UserMapper;
import com.medshop.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运营管理后台控制层（角色 3 运营）。
 * 提供药品库存管理与订单管理能力，所有接口要求运营角色，
 * 越权由 {@link UserContext#requireRole} 拦截返回 403。
 *
 * <ul>
 *   <li>GET  /api/v1/admin/drugs        药品全量列表</li>
 *   <li>POST /api/v1/admin/drugs        新增药品（上架）</li>
 *   <li>PUT  /api/v1/admin/drugs/{id}   编辑药品</li>
 *   <li>PUT  /api/v1/admin/drugs/{id}/stock  调整库存</li>
 *   <li>GET  /api/v1/admin/orders       全部订单</li>
 *   <li>PUT  /api/v1/admin/orders/{id}/status 更新订单状态</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final int ROLE_OPERATOR = 3;

    private final DrugMapper drugMapper;
    private final OrderService orderService;
    private final UserMapper userMapper;
    private final UserService userService;

    public AdminController(DrugMapper drugMapper, OrderService orderService,
                           UserMapper userMapper, UserService userService) {
        this.drugMapper = drugMapper;
        this.orderService = orderService;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    private static final int LOW_STOCK = 60;

    // ---------- 网站统计 ----------

    @GetMapping("/stats")
    public Result<AdminStatsDTO> stats() {
        UserContext.requireRole(ROLE_OPERATOR);
        AdminStatsDTO s = new AdminStatsDTO();

        // 用户（按角色）
        int consumers = userMapper.countByRole(0);
        int pharmacists = userMapper.countByRole(1);
        int staff = userMapper.countByRole(2) + userMapper.countByRole(3);
        s.setUserConsumers(consumers);
        s.setUserPharmacists(pharmacists);
        s.setUserStaff(staff);
        s.setUserTotal(consumers + pharmacists + staff);

        // 订单（按状态）+ 营收
        List<OrderDTO> orders = orderService.listAllOrders();
        BigDecimal revenue = BigDecimal.ZERO;
        for (OrderDTO o : orders) {
            switch (o.getStatus() == null ? -1 : o.getStatus()) {
                case 0 -> s.setOrderPending(s.getOrderPending() + 1);
                case 1 -> s.setOrderPaid(s.getOrderPaid() + 1);
                case 2 -> s.setOrderShipping(s.getOrderShipping() + 1);
                case 3 -> s.setOrderDone(s.getOrderDone() + 1);
                case 4 -> s.setOrderCancelled(s.getOrderCancelled() + 1);
                default -> { }
            }
            // 营收口径：已支付(1)/配送中(2)/已完成(3)计入
            if (o.getStatus() != null && o.getStatus() >= 1 && o.getStatus() <= 3 && o.getTotalAmount() != null) {
                revenue = revenue.add(o.getTotalAmount());
            }
        }
        s.setOrderTotal(orders.size());
        s.setRevenue(revenue);

        // 药品
        List<Drug> drugs = drugMapper.selectAll();
        s.setDrugTotal(drugs.size());
        s.setDrugLowStock((int) drugs.stream()
                .filter(d -> d.getStock() != null && d.getStock() <= LOW_STOCK).count());

        return Result.success(s);
    }

    // ---------- 人员管理 ----------

    @GetMapping("/users")
    public Result<List<UserProfileDTO>> listUsers() {
        UserContext.requireRole(ROLE_OPERATOR);
        return Result.success(userService.listAllUsers());
    }

    @PostMapping("/users")
    public Result<UserProfileDTO> createStaff(@RequestBody StaffReq req) {
        UserContext.requireRole(ROLE_OPERATOR);
        return Result.success(userService.createStaff(req.phone, req.password, req.nickname, req.role));
    }

    // ---------- 药品管理 ----------

    @GetMapping("/drugs")
    public Result<List<Drug>> listDrugs() {
        UserContext.requireRole(ROLE_OPERATOR);
        return Result.success(drugMapper.selectAll());
    }

    @PostMapping("/drugs")
    public Result<Drug> addDrug(@RequestBody Drug drug) {
        UserContext.requireRole(ROLE_OPERATOR);
        if (drug.getStock() == null) drug.setStock(0);
        if (drug.getIsRx() == null) drug.setIsRx(0);
        drugMapper.insert(drug);
        return Result.success(drug);
    }

    @PutMapping("/drugs/{id}")
    public Result<Drug> updateDrug(@PathVariable Long id, @RequestBody Drug drug) {
        UserContext.requireRole(ROLE_OPERATOR);
        drug.setId(id);
        drugMapper.update(drug);
        return Result.success(drug);
    }

    @PutMapping("/drugs/{id}/stock")
    public Result<Void> adjustStock(@PathVariable Long id, @RequestBody @Valid StockReq req) {
        UserContext.requireRole(ROLE_OPERATOR);
        drugMapper.updateStock(id, req.stock);
        return Result.success();
    }

    // ---------- 订单管理 ----------

    @GetMapping("/orders")
    public Result<List<OrderDTO>> listOrders() {
        UserContext.requireRole(ROLE_OPERATOR);
        return Result.success(orderService.listAllOrders());
    }

    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody @Valid StatusReq req) {
        UserContext.requireRole(ROLE_OPERATOR);
        orderService.updateStatus(id, req.status);
        return Result.success();
    }

    // ---------- 请求体 ----------

    public static class StockReq {
        @NotNull(message = "库存不能为空")
        public Integer stock;
    }

    public static class StatusReq {
        @NotNull(message = "状态不能为空")
        public Integer status;
    }

    public static class StaffReq {
        public String phone;
        public String password;
        public String nickname;
        public Integer role; // 1药师 2客服 3运营
    }
}
