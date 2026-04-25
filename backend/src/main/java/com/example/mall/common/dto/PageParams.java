package com.example.mall.common.dto;

/**
 * 分页请求参数
 */
public class PageParams {

    private int page = 1;
    private int pageSize = 10;

    public PageParams() {
    }

    public PageParams(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
