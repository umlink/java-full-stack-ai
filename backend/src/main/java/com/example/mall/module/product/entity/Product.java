package com.example.mall.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品简介/卖点（列表页展示）
     */
    private String brief;

    /**
     * 商品详情（富文本HTML，详情页展示）
     */
    private String description;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 单位：件/箱/斤/台
     */
    private String unit;

    /**
     * 重量（kg）
     */
    private BigDecimal weight;

    /**
     * 是否有SKU：0单规格（直接库存） 1多规格
     */
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

    /**
     * 总库存（冗余，各SKU库存之和或单规格直接库存）
     */
    private Integer totalStock;

    /**
     * 已售数量
     */
    private Integer sales;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品多图（JSON数组）
     */
    private String images;

    /**
     * 商品视频
     */
    private String videoUrl;

    /**
     * 规格模板（JSON）：[{"name":"颜色","values":["银","黑"]}]
     */
    private String specs;

    /**
     * 属性参数（JSON）：[{"name":"屏幕尺寸","value":"6.1英寸"}]
     */
    private String attrs;

    /**
     * 商品标签（JSON）：["新品","热销","推荐"]
     */
    private String tags;

    /**
     * 搜索关键词（逗号分隔，辅助搜索）
     */
    private String keywords;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态：1上架 0下架
     */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
