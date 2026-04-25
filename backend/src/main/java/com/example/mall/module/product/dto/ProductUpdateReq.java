package com.example.mall.module.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 更新商品请求DTO
 */
@Data
public class ProductUpdateReq {

    @NotNull(message = "商品ID不能为空")
    private Long id;

    private String name;

    private String brief;

    private String description;

    private Long categoryId;

    private Long brandId;

    private String unit;

    private BigDecimal weight;

    private Boolean hasSku;

    /**
     * 单规格时直接使用
     */
    private BigDecimal price;

    private Integer totalStock;

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

    /**
     * SKU列表（多规格时必填）
     */
    private List<SkuItem> skus;

    @Data
    public static class SkuItem {

        /**
         * SKU ID，null表示新增，非null表示更新
         */
        private Long id;

        private String name;

        private Map<String, String> attrs;

        private BigDecimal price;

        private Integer stock;

        private String code;

        private String image;

        private BigDecimal weight;

        private Integer status;

        private Integer sortOrder;
    }
}
