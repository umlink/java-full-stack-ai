package com.example.mall.module.order.controller;

import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.order.dto.OrderVO;
import com.example.mall.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<OrderVO> result = orderService.pageByAdmin(status, keyword, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getDetail(@PathVariable Long id) {
        OrderVO detail = orderService.getDetailAdmin(id);
        return Result.success(detail);
    }

    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.ship(id);
        return Result.success();
    }
}
