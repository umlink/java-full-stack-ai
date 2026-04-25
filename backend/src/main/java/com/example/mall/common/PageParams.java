package com.example.mall.common;

import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageParams {

    /**
     * 当前页码，从 1 开始
     */
    private int page = 1;

    /**
     * 每页条数
     */
    private int pageSize = 10;

    public PageParams() {
    }

    public PageParams(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 转换为 MyBatis-Plus Page 对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toMyBatisPlusPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
    }
}
