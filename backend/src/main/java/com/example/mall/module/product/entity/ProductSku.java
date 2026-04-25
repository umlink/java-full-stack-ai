package com.example.mall.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品SKU实体
 */
@Data
@TableName("product_sku")
public class ProductSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属商品ID
     */
    private Long productId;

    /**
     * SKU名称："银色 128G"
     */
    private String name;

    /**
     * SKU属性组合（JSON）：{"颜色":"银色","存储":"128G"}
     */
    private String attrs;

    /**
     * SKU售价
     */
    private BigDecimal price;

    /**
     * SKU库存
     */
    private Integer stock;

    /**
     * SKU编码/货号（唯一）
     */
    private String code;

    /**
     * SKU专属图片（覆盖商品主图）
     */
    private String image;

    /**
     * SKU重量（覆盖商品重量）
     */
    private BigDecimal weight;

    /**
     * 状态：1启用 0禁用
     */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    /**
     * 排序值
     */
    private Integer sortOrder;

    private Date createdAt;

    private Date updatedAt;
}
