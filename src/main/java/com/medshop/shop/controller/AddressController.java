package com.medshop.shop.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.shop.entity.Address;
import com.medshop.shop.service.AddressService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址控制层（任意登录用户管理自己的地址）。
 * <ul>
 *   <li>GET    /api/v1/addresses           我的地址列表</li>
 *   <li>POST   /api/v1/addresses           新增地址</li>
 *   <li>PUT    /api/v1/addresses/{id}       编辑地址</li>
 *   <li>DELETE /api/v1/addresses/{id}       删除地址</li>
 *   <li>PUT    /api/v1/addresses/{id}/default 设为默认</li>
 * </ul>
 * 归属以登录用户为边界，无法操作他人地址。
 */
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<Address>> list() {
        return Result.success(addressService.list(UserContext.requireUserId()));
    }

    @PostMapping
    public Result<Address> create(@RequestBody Address addr) {
        return Result.success(addressService.create(UserContext.requireUserId(), addr));
    }

    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @RequestBody Address addr) {
        return Result.success(addressService.update(UserContext.requireUserId(), id, addr));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.requireUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.requireUserId(), id);
        return Result.success();
    }
}
