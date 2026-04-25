package com.example.mall.module.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.module.order.entity.Orders;
import com.example.mall.module.order.mapper.OrderMapper;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    public Map<String, Object> getOverview() {
        // 总订单数 + 总销售额
        List<Orders> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>().ne(Orders::getStatus, 4));
        long totalOrders = allOrders.size();
        BigDecimal totalSales = allOrders.stream()
                .filter(o -> o.getStatus() >= 1)
                .map(Orders::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 今日数据
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Orders> todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toInstant().isBefore(java.sql.Timestamp.valueOf(todayStart).toInstant()))
                .collect(Collectors.toList());
        BigDecimal todaySales = todayOrders.stream()
                .filter(o -> o.getStatus() >= 1)
                .map(Orders::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalSales", totalSales);
        result.put("todayOrders", todayOrders.size());
        result.put("todaySales", todaySales);
        return result;
    }

    public List<Map<String, Object>> getProductRanking(int limit) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", i + 1);
            item.put("productId", p.getId());
            item.put("productName", p.getName());
            item.put("sales", p.getSales());
            item.put("price", p.getPrice());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getOrderTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 获取所有非取消订单
        List<Orders> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .ne(Orders::getStatus, 4));

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

            long count = orders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .filter(o -> o.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(date))
                    .count();

            BigDecimal amount = orders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getStatus() >= 1)
                    .filter(o -> o.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(date))
                    .map(Orders::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("orderCount", count);
            item.put("amount", amount);
            result.add(item);
        }
        return result;
    }
}
