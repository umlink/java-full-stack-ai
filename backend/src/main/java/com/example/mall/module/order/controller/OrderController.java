package com.example.mall.module.order.controller;

import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.order.dto.CreateOrderReq;
import com.example.mall.module.order.dto.OrderVO;
import com.example.mall.module.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody CreateOrderReq req) {
        Long userId = getCurrentUserId();
        OrderVO order = orderService.createOrder(userId, req);
        return Result.success(order);
    }

    @GetMapping
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getCurrentUserId();
        PageResult<OrderVO> result = orderService.pageByUser(userId, status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getDetail(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        OrderVO detail = orderService.getDetail(userId, id);
        return Result.success(detail);
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        orderService.pay(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        orderService.cancel(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        orderService.confirmReceipt(userId, id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        throw new RuntimeException("无法获取用户信息");
    }
}
