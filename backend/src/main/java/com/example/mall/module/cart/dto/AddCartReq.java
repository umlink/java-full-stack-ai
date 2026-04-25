package com.example.mall.module.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加购物车请求
 */
@Data
public class AddCartReq {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * SKU ID（多规格商品必填，单规格商品可为空）
     */
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "单次购买最多99件")
    private Integer quantity;
}
