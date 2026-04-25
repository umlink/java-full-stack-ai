package com.example.mall.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.common.PageResult;
import com.example.mall.module.cart.dto.CartVO;
import com.example.mall.module.cart.entity.CartItem;
import com.example.mall.module.cart.mapper.CartMapper;
import com.example.mall.module.order.dto.CreateOrderReq;
import com.example.mall.module.order.dto.OrderVO;
import com.example.mall.module.order.entity.OrderItem;
import com.example.mall.module.order.entity.Orders;
import com.example.mall.module.order.mapper.OrderItemMapper;
import com.example.mall.module.order.mapper.OrderMapper;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.entity.ProductSku;
import com.example.mall.module.product.mapper.ProductMapper;
import com.example.mall.module.product.mapper.ProductSkuMapper;
import com.example.mall.module.user.entity.UserAddress;
import com.example.mall.module.user.mapper.UserAddressMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserAddressMapper addressMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建订单
     */
    public OrderVO createOrder(Long userId, CreateOrderReq req) {
        // 获取购物车商品
        List<CartVO> cartItems = cartMapper.selectCartWithProduct(userId);
        Set<Long> requestedIds = new HashSet<>(req.getCartItemIds());

        List<CartVO> selectedItems = cartItems.stream()
                .filter(i -> requestedIds.contains(i.getId()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "请选择有效商品");
        }

        // 校验商品状态
        for (CartVO item : selectedItems) {
            if (Boolean.TRUE.equals(item.getIsOffline())) {
                throw new BusinessException(BizCode.PRODUCT_OFFLINE.getCode(),
                        "商品「" + item.getProductName() + "」已下架");
            }
            if (item.getStock() <= 0 || item.getQuantity() > item.getStock()) {
                throw new BusinessException(BizCode.STOCK_NOT_ENOUGH.getCode(),
                        "商品「" + item.getProductName() + "」库存不足");
            }
        }

        // FOR UPDATE 锁定库存行
        for (CartVO item : selectedItems) {
            Product product = productMapper.selectByIdForUpdate(item.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BusinessException(BizCode.PRODUCT_OFFLINE.getCode(),
                        "商品「" + item.getProductName() + "」已下架");
            }
            if (item.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectByIdForUpdate(item.getSkuId());
                if (sku == null || sku.getStock() < item.getQuantity()) {
                    throw new BusinessException(BizCode.STOCK_NOT_ENOUGH.getCode(),
                            "商品「" + item.getProductName() + "」库存不足");
                }
            }
        }

        // 扣减库存
        for (CartVO item : selectedItems) {
            Product product = productMapper.selectById(item.getProductId());
            product.setTotalStock(product.getTotalStock() - item.getQuantity());
            product.setSales(product.getSales() + item.getQuantity());
            productMapper.updateById(product);

            if (item.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                sku.setStock(sku.getStock() - item.getQuantity());
                productSkuMapper.updateById(sku);
            }
        }

        // 地址快照
        UserAddress address = addressMapper.selectById(req.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.ADDRESS_NOT_FOUND);
        }

        Map<String, Object> addressSnapshot = new LinkedHashMap<>();
        addressSnapshot.put("receiverName", address.getReceiverName());
        addressSnapshot.put("receiverPhone", address.getReceiverPhone());
        addressSnapshot.put("province", address.getProvince());
        addressSnapshot.put("city", address.getCity());
        addressSnapshot.put("district", address.getDistrict());
        addressSnapshot.put("detail", address.getDetail());

        // 创建订单
        String orderNo = generateOrderNo();
        BigDecimal totalAmount = selectedItems.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setRemark(req.getRemark());
        try {
            order.setAddressSnapshot(objectMapper.writeValueAsString(addressSnapshot));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("地址快照序列化失败", e);
        }
        orderMapper.insert(order);

        // 订单商品快照
        for (CartVO item : selectedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setProductImage(item.getProductImage());
            orderItem.setSkuId(item.getSkuId());
            orderItem.setSkuName(item.getSkuName());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        // 清除购物车已选
        List<CartItem> cartItemEntities = cartMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .in(CartItem::getId, req.getCartItemIds())
                        .eq(CartItem::getUserId, userId)
        );
        for (CartItem ci : cartItemEntities) {
            cartMapper.deleteById(ci.getId());
        }

        return buildOrderVO(order);
    }

    /**
     * 用户订单列表（分页）
     */
    public PageResult<OrderVO> pageByUser(Long userId, Integer status, int pageNum, int pageSize) {
        Page<Orders> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreatedAt);

        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }

        IPage<Orders> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> list = result.getRecords().stream()
                .map(this::buildOrderVO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 管理员订单列表
     */
    public PageResult<OrderVO> pageByAdmin(Integer status, String keyword, int pageNum, int pageSize) {
        Page<Orders> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .orderByDesc(Orders::getCreatedAt);

        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Orders::getOrderNo, keyword);
        }

        IPage<Orders> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> list = result.getRecords().stream()
                .map(this::buildOrderVO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 订单详情
     */
    public OrderVO getDetail(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        return buildOrderVO(order);
    }

    /**
     * 管理员查询订单详情
     */
    public OrderVO getDetailAdmin(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        return buildOrderVO(order);
    }

    /**
     * 模拟支付
     */
    public void pay(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(BizCode.ORDER_ALREADY_PAID);
        }
        order.setStatus(1);
        order.setPaymentMethod("SIMULATED");
        order.setPaidAt(new Date());
        orderMapper.updateById(order);
    }

    /**
     * 取消订单
     */
    public void cancel(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(BizCode.ORDER_CANNOT_CANCEL);
        }
        // 检查是否在 30 分钟内
        long minutes = java.time.Duration.between(
                order.getCreatedAt().toInstant(),
                new Date().toInstant()
        ).toMinutes();
        if (minutes > 30) {
            throw new BusinessException(BizCode.ORDER_TIMEOUT);
        }

        // 归还库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setTotalStock(product.getTotalStock() + item.getQuantity());
                productMapper.updateById(product);
            }
            if (item.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                if (sku != null) {
                    sku.setStock(sku.getStock() + item.getQuantity());
                    productSkuMapper.updateById(sku);
                }
            }
        }

        order.setStatus(4);
        order.setCancelReason("用户取消");
        order.setCanceledAt(new Date());
        orderMapper.updateById(order);
    }

    /**
     * 确认收货
     */
    public void confirmReceipt(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(BizCode.ORDER_CANNOT_CANCEL.getCode(), "当前订单状态不可确认收货");
        }
        order.setStatus(3);
        order.setCompletedAt(new Date());
        orderMapper.updateById(order);
    }

    /**
     * 管理员发货
     */
    public void ship(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(BizCode.ORDER_ALREADY_SHIPPED);
        }
        order.setStatus(2);
        order.setShippedAt(new Date());
        orderMapper.updateById(order);
    }

    private String generateOrderNo() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = String.format("%06d", (int) (Math.random() * 1000000));
        return timestamp + suffix;
    }

    private OrderVO buildOrderVO(Orders order) {
        // 查询商品项
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPaidAt(order.getPaidAt());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setCanceledAt(order.getCanceledAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setIsFlashSale(order.getIsFlashSale());
        vo.setCreatedAt(order.getCreatedAt());

        // 商品项
        List<OrderVO.OrderItemVO> itemVOs = items.stream().map(i -> {
            OrderVO.OrderItemVO oi = new OrderVO.OrderItemVO();
            oi.setId(i.getId());
            oi.setProductId(i.getProductId());
            oi.setProductName(i.getProductName());
            oi.setProductImage(i.getProductImage());
            oi.setSkuId(i.getSkuId());
            oi.setSkuName(i.getSkuName());
            oi.setPrice(i.getPrice());
            oi.setQuantity(i.getQuantity());
            oi.setSubtotal(i.getSubtotal());
            return oi;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        // 地址快照
        if (order.getAddressSnapshot() != null) {
            try {
                Map<String, Object> addrMap = objectMapper.readValue(
                        order.getAddressSnapshot(), new TypeReference<Map<String, Object>>() {});
                OrderVO.AddressSnapshotVO addrVO = new OrderVO.AddressSnapshotVO();
                addrVO.setReceiverName((String) addrMap.get("receiverName"));
                addrVO.setReceiverPhone((String) addrMap.get("receiverPhone"));
                addrVO.setProvince((String) addrMap.get("province"));
                addrVO.setCity((String) addrMap.get("city"));
                addrVO.setDistrict((String) addrMap.get("district"));
                addrVO.setDetail((String) addrMap.get("detail"));
                vo.setAddressSnapshot(addrVO);
            } catch (Exception ignored) {}
        }

        return vo;
    }
}
