package com.example.mall.module.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品查询请求参数
 */
@Data
public class ProductQueryReq {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 搜索关键词（全文搜索）
     */
    private String keyword;

    /**
     * 排序：price_asc / price_desc / sales_desc / new_desc
     */
    private String sort;

    /**
     * 状态（管理端使用，公开端固定为1）
     */
    private Integer status;

    private Integer page = 1;

    private Integer pageSize = 20;
}
