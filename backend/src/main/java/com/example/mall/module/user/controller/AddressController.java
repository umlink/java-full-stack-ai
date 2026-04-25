package com.example.mall.module.user.controller;

import com.example.mall.common.Result;
import com.example.mall.module.user.entity.UserAddress;
import com.example.mall.module.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * 收货地址控制器（需登录认证）
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 获取当前用户的地址列表
     */
    @GetMapping
    public Result<List<UserAddress>> list() {
        Long userId = getCurrentUserId();
        List<UserAddress> list = addressService.getMyAddresses(userId);
        return Result.success(list);
    }

    /**
     * 获取单个地址
     */
    @GetMapping("/{id}")
    public Result<UserAddress> getById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        UserAddress address = addressService.getById(id, userId);
        return Result.success(address);
    }

    /**
     * 新增地址
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserAddress address) {
        Long userId = getCurrentUserId();
        addressService.create(address, userId);
        return Result.success();
    }

    /**
     * 更新地址
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserAddress address) {
        Long userId = getCurrentUserId();
        address.setId(id);
        addressService.update(address, userId);
        return Result.success();
    }

    /**
     * 设为默认地址
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        addressService.setDefault(id, userId);
        return Result.success();
    }

    /**
     * 删除地址（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        addressService.delete(id, userId);
        return Result.success();
    }

    /**
     * 从 SecurityContextHolder 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
