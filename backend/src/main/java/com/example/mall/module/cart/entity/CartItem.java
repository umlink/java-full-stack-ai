package com.example.mall.module.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 购物车项实体
 */
@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID（多规格时必填，单规格为 NULL）
     */
    private Long skuId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否选中：1选中 0未选
     */
    private Boolean selected;

    private Date createdAt;

    private Date updatedAt;
}
