package com.example.mall.module.cart.controller;

import com.example.mall.common.Result;
import com.example.mall.module.cart.dto.AddCartReq;
import com.example.mall.module.cart.dto.CartVO;
import com.example.mall.module.cart.service.CartService;
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
import java.util.Map;

/**
 * 购物车控制器（需登录认证）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 获取购物车列表
     */
    @GetMapping
    public Result<List<CartVO>> getCart() {
        Long userId = getCurrentUserId();
        List<CartVO> cartList = cartService.getMyCart(userId);
        return Result.success(cartList);
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Void> addItem(@Valid @RequestBody AddCartReq req) {
        Long userId = getCurrentUserId();
        cartService.addItem(userId, req);
        return Result.success();
    }

    /**
     * 修改购物车项数量
     *
     * @param id       购物车项ID
     * @param body     JSON 请求体：{"quantity": N}
     */
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id,
                                       @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        if (quantity == null) {
            return Result.error(400, "缺少 quantity 参数");
        }
        Long userId = getCurrentUserId();
        cartService.updateQuantity(userId, id, quantity);
        return Result.success();
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        cartService.deleteItem(userId, id);
        return Result.success();
    }

    /**
     * 清空已选商品
     */
    @DeleteMapping("/selected")
    public Result<Void> clearSelected() {
        Long userId = getCurrentUserId();
        cartService.clearSelected(userId);
        return Result.success();
    }

    /**
     * 从 SecurityContextHolder 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
