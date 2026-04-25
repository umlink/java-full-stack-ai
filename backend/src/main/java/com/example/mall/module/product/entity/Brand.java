package com.example.mall.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 品牌实体
 */
@Data
@TableName("brand")
public class Brand {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 品牌名称（唯一）
     */
    private String name;

    /**
     * 品牌Logo URL
     */
    private String logo;

    /**
     * 品牌描述
     */
    private String description;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    private Date createdAt;
}
