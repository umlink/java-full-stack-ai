package com.example.mall.module.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车展示 VO
 */
@Data
public class CartVO {

    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    private Long skuId;

    private String skuName;

    /**
     * 实际售价（SKU 价或商品价）
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 可用库存（SKU 库存或商品总库存）
     */
    private Integer stock;

    /**
     * 是否选中
     */
    private Boolean selected;

    /**
     * 商品是否已下架
     */
    private Boolean isOffline;
}
