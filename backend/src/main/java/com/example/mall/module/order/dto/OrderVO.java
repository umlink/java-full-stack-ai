package com.example.mall.module.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private String paymentMethod;
    private Date paidAt;
    private String remark;
    private String cancelReason;
    private Date canceledAt;
    private Date shippedAt;
    private Date completedAt;
    private Integer isFlashSale;
    private Date createdAt;

    private List<OrderItemVO> items;
    private AddressSnapshotVO addressSnapshot;

    @Data
    public static class OrderItemVO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private Long skuId;
        private String skuName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    @Data
    public static class AddressSnapshotVO {
        private String receiverName;
        private String receiverPhone;
        private String province;
        private String city;
        private String district;
        private String detail;
    }
}
