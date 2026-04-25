package com.example.mall.module.flashsale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.common.PageResult;
import com.example.mall.module.flashsale.dto.CreateFlashEventReq;
import com.example.mall.module.flashsale.dto.FlashSaleEventVO;
import com.example.mall.module.flashsale.dto.FlashSaleItemVO;
import com.example.mall.module.flashsale.entity.FlashSaleEvent;
import com.example.mall.module.flashsale.entity.FlashSaleItem;
import com.example.mall.module.flashsale.mapper.FlashSaleEventMapper;
import com.example.mall.module.flashsale.mapper.FlashSaleItemMapper;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleEventMapper eventMapper;
    private final FlashSaleItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FlashOrderConsumerService flashOrderConsumerService;

    /**
     * 启动时预热所有进行中的活动到 Redis，防止种子数据直接导入后跳过预热
     */
    @PostConstruct
    public void preheatActiveEvents() {
        List<FlashSaleEvent> activeEvents = eventMapper.selectList(
                new LambdaQueryWrapper<FlashSaleEvent>().eq(FlashSaleEvent::getStatus, 1));
        for (FlashSaleEvent event : activeEvents) {
            preheatEvent(event.getId());
        }
    }

    // ========== Event CRUD ==========

    public PageResult<FlashSaleEventVO> pageEvents(int pageNum, int pageSize, Integer status) {
        Page<FlashSaleEvent> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlashSaleEvent> wrapper = new LambdaQueryWrapper<FlashSaleEvent>()
                .orderByDesc(FlashSaleEvent::getCreatedAt);
        if (status != null) wrapper.eq(FlashSaleEvent::getStatus, status);

        IPage<FlashSaleEvent> result = eventMapper.selectPage(page, wrapper);
        List<FlashSaleEventVO> list = result.getRecords().stream()
                .map(this::toEventVO).collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), pageNum, pageSize);
    }

    public FlashSaleEventVO getEventDetail(Long eventId) {
        FlashSaleEvent event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "活动不存在");
        return toEventVO(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createEvent(CreateFlashEventReq req) {
        if (req.getEndTime().before(req.getStartTime())) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "结束时间不能早于开始时间");
        }

        FlashSaleEvent event = new FlashSaleEvent();
        event.setName(req.getName());
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        event.setStatus(0); // 待开始
        event.setRemark(req.getRemark());
        eventMapper.insert(event);

        if (req.getItems() != null) {
            for (CreateFlashEventReq.FlashItemReq itemReq : req.getItems()) {
                // 校验同商品不能在多个进行中的活动
                Long conflict = itemMapper.selectCount(new LambdaQueryWrapper<FlashSaleItem>()
                        .eq(FlashSaleItem::getProductId, itemReq.getProductId())
                        .in(FlashSaleItem::getEventId,
                                new LambdaQueryWrapper<FlashSaleEvent>()
                                        .in(FlashSaleEvent::getStatus, 0, 1)
                                        .select(FlashSaleEvent::getId)));
                if (conflict > 0) {
                    throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "该商品已在其他进行中的活动");
                }

                FlashSaleItem item = new FlashSaleItem();
                item.setEventId(event.getId());
                item.setProductId(itemReq.getProductId());
                item.setFlashPrice(itemReq.getFlashPrice());
                item.setFlashStock(itemReq.getFlashStock());
                item.setLimitPerUser(itemReq.getLimitPerUser());
                itemMapper.insert(item);
            }
        }
    }

    public void updateEventStatus(Long eventId, Integer status) {
        FlashSaleEvent event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "活动不存在");
        event.setStatus(status);
        eventMapper.updateById(event);
    }

    // ========== Public query ==========

    public List<FlashSaleEventVO> getActiveEvents() {
        Date now = new Date();
        List<FlashSaleEvent> events = eventMapper.selectList(new LambdaQueryWrapper<FlashSaleEvent>()
                .eq(FlashSaleEvent::getStatus, 1)
                .or(w -> w.eq(FlashSaleEvent::getStatus, 0)
                        .le(FlashSaleEvent::getStartTime, new Date(now.getTime() + 30 * 60 * 1000)))
                .orderByAsc(FlashSaleEvent::getStartTime));
        return events.stream().map(this::toEventVO).collect(Collectors.toList());
    }

    // ========== Core flash sale ==========

    /**
     * 预扣库存并发送 MQ 消息
     */
    public Map<String, Object> tryFlashOrder(Long userId, Long eventId, Long itemId) {
        // 1. 限流检查（1s 间隔）
        String rateKey = "flash:rate:" + userId + ":" + itemId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", 1, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            throw new BusinessException(BizCode.RATE_LIMIT);
        }

        // 2. 从 Redis 获取活动信息
        String eventKey = "flash:event:" + eventId;
        Map<Object, Object> eventData = redisTemplate.opsForHash().entries(eventKey);
        if (eventData.isEmpty()) {
            throw new BusinessException(BizCode.FLASH_SALE_NOT_STARTED);
        }

        String statusStr = (String) eventData.get("status");
        if (!"1".equals(statusStr)) {
            throw new BusinessException(BizCode.FLASH_SALE_ENDED);
        }

        // 3. 限购校验
        String userSetKey = "flash:users:" + itemId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(isMember)) {
            throw new BusinessException(BizCode.FLASH_SALE_LIMIT);
        }

        // 4. Redis 预扣库存
        String stockKey = "flash:stock:" + itemId;
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            // 回滚
            redisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException(BizCode.SOLD_OUT);
        }

        // 5. 生成请求 ID 并缓存上下文，由 MQ consumer 处理后续逻辑
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String resultKey = "flash:result:" + requestId;
        Map<String, String> ctx = new HashMap<>();
        ctx.put("userId", String.valueOf(userId));
        ctx.put("eventId", String.valueOf(eventId));
        ctx.put("itemId", String.valueOf(itemId));
        ctx.put("status", "pending");
        try {
            redisTemplate.opsForValue().set(resultKey,
                    objectMapper.writeValueAsString(ctx), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存秒杀上下文失败", e);
        }

        // 6. 发送到 RocketMQ (由 consumer 做后续下单)
        // 使用 Redis pub/sub 或直接调用下单逻辑（无 RocketMQ 环境时）
        // 简化为直接同步下单
        try {
            flashOrderConsumerService.processOrder(userId, eventId, itemId, requestId);
        } catch (Exception e) {
            ctx.put("status", "failed");
            ctx.put("error", e.getMessage());
            try {
                redisTemplate.opsForValue().set(resultKey,
                        objectMapper.writeValueAsString(ctx), 5, TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return Map.of("requestId", requestId);
    }

    /**
     * 处理秒杀下单（由 MQ consumer 或直接调用）
     */
    // 实际下单逻辑在 FlashOrderConsumerService.processOrder() 中处理

    /**
     * 查询秒杀结果
     */
    public Map<String, Object> getOrderStatus(String requestId) {
        String resultKey = "flash:result:" + requestId;
        String json = redisTemplate.opsForValue().get(resultKey);
        if (json == null) {
            return Map.of("status", "not_found");
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("status", "error");
        }
    }

    // ========== Scheduled tasks ==========

    public void autoUpdateEventStatus() {
        Date now = new Date();
        // 启动到达开始时间的活动
        List<FlashSaleEvent> toStart = eventMapper.selectList(new LambdaQueryWrapper<FlashSaleEvent>()
                .eq(FlashSaleEvent::getStatus, 0)
                .le(FlashSaleEvent::getStartTime, now));
        for (FlashSaleEvent event : toStart) {
            event.setStatus(1);
            eventMapper.updateById(event);
            preheatEvent(event.getId());
        }

        // 结束到达结束时间的活动
        List<FlashSaleEvent> toEnd = eventMapper.selectList(new LambdaQueryWrapper<FlashSaleEvent>()
                .eq(FlashSaleEvent::getStatus, 1)
                .le(FlashSaleEvent::getEndTime, now));
        for (FlashSaleEvent event : toEnd) {
            event.setStatus(2);
            eventMapper.updateById(event);
        }
    }

    /**
     * 预热活动数据到 Redis
     */
    public void preheatEvent(Long eventId) {
        FlashSaleEvent event = eventMapper.selectById(eventId);
        if (event == null) return;

        // 缓存活动信息
        String eventKey = "flash:event:" + eventId;
        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("status", String.valueOf(event.getStatus()));
        eventMap.put("startTime", String.valueOf(event.getStartTime().getTime()));
        eventMap.put("endTime", String.valueOf(event.getEndTime().getTime()));
        redisTemplate.opsForHash().putAll(eventKey, eventMap);

        // 缓存秒杀库存
        List<FlashSaleItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<FlashSaleItem>().eq(FlashSaleItem::getEventId, eventId));
        for (FlashSaleItem item : items) {
            String stockKey = "flash:stock:" + item.getId();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(item.getFlashStock()));
            // 缓存商品信息
            String itemKey = "flash:item:" + item.getId();
            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("flashPrice", item.getFlashPrice().toString());
            itemMap.put("limitPerUser", String.valueOf(item.getLimitPerUser()));
            redisTemplate.opsForHash().putAll(itemKey, itemMap);
        }
    }

    // ========== Admin queries ==========

    public void deleteEvent(Long eventId) {
        eventMapper.deleteById(eventId);
        itemMapper.delete(new LambdaQueryWrapper<FlashSaleItem>()
                .eq(FlashSaleItem::getEventId, eventId));
    }

    // ========== Helpers ==========

    private FlashSaleEventVO toEventVO(FlashSaleEvent event) {
        FlashSaleEventVO vo = new FlashSaleEventVO();
        vo.setId(event.getId());
        vo.setName(event.getName());
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setStatus(event.getStatus());
        vo.setRemark(event.getRemark());
        vo.setCreatedAt(event.getCreatedAt());

        List<FlashSaleItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<FlashSaleItem>().eq(FlashSaleItem::getEventId, event.getId()));
        vo.setItems(items.stream().map(item -> {
            FlashSaleItemVO itemVO = new FlashSaleItemVO();
            itemVO.setId(item.getId());
            itemVO.setEventId(item.getEventId());
            itemVO.setProductId(item.getProductId());
            itemVO.setFlashPrice(item.getFlashPrice());
            itemVO.setFlashStock(item.getFlashStock());
            itemVO.setLimitPerUser(item.getLimitPerUser());
            itemVO.setVersion(item.getVersion());

            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                itemVO.setProductName(product.getName());
                itemVO.setProductImage(product.getMainImage());
                itemVO.setOriginalPrice(product.getPrice());
            }
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }
}
