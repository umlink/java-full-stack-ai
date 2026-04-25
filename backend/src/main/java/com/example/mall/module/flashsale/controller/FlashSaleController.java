package com.example.mall.module.flashsale.controller;

import com.example.mall.common.Result;
import com.example.mall.module.flashsale.dto.FlashSaleEventVO;
import com.example.mall.module.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/events")
    public Result<List<FlashSaleEventVO>> getActiveEvents() {
        return Result.success(flashSaleService.getActiveEvents());
    }

    @GetMapping("/events/{id}")
    public Result<FlashSaleEventVO> getEventDetail(@PathVariable Long id) {
        return Result.success(flashSaleService.getEventDetail(id));
    }

    @PostMapping("/order")
    public Result<Map<String, Object>> flashOrder(@RequestParam Long eventId, @RequestParam Long itemId) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = flashSaleService.tryFlashOrder(userId, eventId, itemId);
        return Result.success(result);
    }

    @GetMapping("/order/status")
    public Result<Map<String, Object>> orderStatus(@RequestParam String requestId) {
        return Result.success(flashSaleService.getOrderStatus(requestId));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        throw new RuntimeException("无法获取用户信息");
    }
}
