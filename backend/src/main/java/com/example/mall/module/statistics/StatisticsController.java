package com.example.mall.module.statistics;

import com.example.mall.common.Result;
import com.example.mall.module.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statisticsService.getOverview());
    }

    @GetMapping("/product-ranking")
    public Result<List<Map<String, Object>>> productRanking(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsService.getProductRanking(limit));
    }

    @GetMapping("/order-trend")
    public Result<List<Map<String, Object>>> orderTrend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.getOrderTrend(days));
    }
}
