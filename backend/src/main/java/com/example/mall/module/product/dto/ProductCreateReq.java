package com.example.mall.module.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 创建商品请求DTO
 */
@Data
public class ProductCreateReq {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String brief;

    private String description;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long brandId;

    private String unit = "件";

    private BigDecimal weight;

    private Boolean hasSku = false;

    /**
     * 单规格时直接使用
     */
    private BigDecimal price;

    private Integer totalStock;

    @NotBlank(message = "商品主图不能为空")
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

    private Integer sortOrder = 0;

    /**
     * SKU列表（多规格时必填）
     */
    private List<SkuItem> skus;

    @Data
    public static class SkuItem {

        private String name;

        private Map<String, String> attrs;

        @NotNull(message = "SKU价格不能为空")
        private BigDecimal price;

        @NotNull(message = "SKU库存不能为空")
        private Integer stock;

        private String code;

        private String image;

        private BigDecimal weight;

        private Integer sortOrder = 0;
    }
}
