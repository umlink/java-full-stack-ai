package com.example.mall.module.flashsale.service;

import com.example.mall.module.flashsale.entity.FlashSaleItem;
import com.example.mall.module.flashsale.mapper.FlashSaleItemMapper;
import com.example.mall.module.order.entity.OrderItem;
import com.example.mall.module.order.entity.Orders;
import com.example.mall.module.order.mapper.OrderItemMapper;
import com.example.mall.module.order.mapper.OrderMapper;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashOrderConsumerService {

    private final StringRedisTemplate redisTemplate;
    private final FlashSaleItemMapper flashSaleItemMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Long userId, Long eventId, Long itemId, String requestId) {
        // 1. 幂等校验
        String dedupKey = "flash:dedup:" + requestId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(set)) {
            log.warn("重复处理秒杀订单 requestId={}", requestId);
            return;
        }

        // 2. 乐观锁扣库存（重试 5 次）
        FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
        if (item == null || item.getFlashStock() <= 0) {
            updateResult(requestId, "failed", "已售罄");
            return;
        }

        boolean stockDeducted = false;
        for (int retry = 0; retry < 5; retry++) {
            item = flashSaleItemMapper.selectById(itemId);
            if (item.getFlashStock() <= 0) break;

            item.setFlashStock(item.getFlashStock() - 1);
            int affected = flashSaleItemMapper.updateById(item);
            if (affected > 0) {
                stockDeducted = true;
                break;
            }
            // 等待后重试
            try {
                Thread.sleep(Math.min(50L * (1 << retry), 2000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!stockDeducted) {
            updateResult(requestId, "failed", "已售罄");
            return;
        }

        // 3. 查询商品信息
        Product product = productMapper.selectById(item.getProductId());
        if (product == null) {
            updateResult(requestId, "failed", "商品不存在");
            return;
        }

        // 4. 创建订单
        String orderNo = generateOrderNo();
        BigDecimal totalAmount = item.getFlashPrice();

        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setIsFlashSale(1);
        orderMapper.insert(order);

        // 5. 创建订单商品快照
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setProductImage(product.getMainImage());
        orderItem.setPrice(item.getFlashPrice());
        orderItem.setQuantity(1);
        orderItem.setSubtotal(item.getFlashPrice());
        orderItemMapper.insert(orderItem);

        // 6. 记录用户已秒杀
        redisTemplate.opsForSet().add("flash:users:" + itemId, String.valueOf(userId));

        // 7. 更新结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("orderNo", orderNo);
        result.put("orderId", order.getId());
        try {
            redisTemplate.opsForValue().set("flash:result:" + requestId,
                    objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("更新秒杀结果失败", e);
        }
    }

    private void updateResult(String requestId, String status, String msg) {
        Map<String, String> result = new HashMap<>();
        result.put("status", status);
        result.put("message", msg);
        try {
            redisTemplate.opsForValue().set("flash:result:" + requestId,
                    objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("更新秒杀结果失败", e);
        }
    }

    private String generateOrderNo() {
        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "FS" + ts + String.format("%06d", (int) (Math.random() * 1000000));
    }
}
