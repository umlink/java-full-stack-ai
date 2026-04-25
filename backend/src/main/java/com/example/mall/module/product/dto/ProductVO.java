package com.example.mall.module.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 商品列表展示VO
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    private String brief;

    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    private Long brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    private String unit;

    private Boolean hasSku;

    /**
     * 显示价格（单规格=售价，多规格=起售价）
     */
    private BigDecimal price;

    /**
     * SKU最低价（多规格时使用）
     */
    private BigDecimal minPrice;

    /**
     * SKU最高价（多规格时使用）
     */
    private BigDecimal maxPrice;

    private Integer totalStock;

    private Integer sales;

    private String mainImage;

    /**
     * 商品多图列表
     */
    private List<String> images;

    /**
     * 标签列表
     */
    private List<String> tags;

    private String keywords;

    private Integer sortOrder;

    private Integer status;

    private Date createdAt;
}
