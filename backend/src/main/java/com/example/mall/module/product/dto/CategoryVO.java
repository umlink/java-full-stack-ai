package com.example.mall.module.product.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private Integer sortOrder;

    private Integer status;

    /**
     * 子分类列表
     */
    private List<CategoryVO> children;
}
