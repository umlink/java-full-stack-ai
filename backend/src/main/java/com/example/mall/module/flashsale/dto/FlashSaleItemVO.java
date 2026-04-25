package com.example.mall.module.flashsale.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlashSaleItemVO {

    private Long id;
    private Long eventId;
    private Long productId;
    private BigDecimal flashPrice;
    private Integer flashStock;
    private Integer limitPerUser;
    private Integer version;

    // 关联商品信息
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
}
