package com.example.mall.module.product.dto;

import com.example.mall.module.product.entity.ProductSku;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 商品详情VO（含品牌名、分类名、SKU列表、规格模板、属性参数）
 */
@Data
public class ProductDetailVO {

    private Long id;

    private String name;

    private String brief;

    private String description;

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

    private BigDecimal weight;

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

    private String videoUrl;

    /**
     * 规格模板：[{"name":"颜色","values":["银","黑"]}]
     */
    private List<Map<String, Object>> specs;

    /**
     * 属性参数：[{"name":"屏幕尺寸","value":"6.1英寸"}]
     */
    private List<Map<String, Object>> attrs;

    /**
     * 标签列表
     */
    private List<String> tags;

    private String keywords;

    private Integer sortOrder;

    private Integer status;

    /**
     * SKU列表（多规格商品时返回）
     */
    private List<ProductSku> skus;

    private Date createdAt;

    private Date updatedAt;
}
