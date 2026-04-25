package com.example.mall.module.product.controller;

import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.product.dto.ProductDetailVO;
import com.example.mall.module.product.dto.ProductQueryReq;
import com.example.mall.module.product.dto.ProductVO;
import com.example.mall.module.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品公开接口（无需认证）
 *
 * <p>提供商品列表和详情查询，供前端商品浏览等场景使用。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品分页列表（含条件筛选/搜索/排序）
     */
    @GetMapping
    public Result<PageResult<ProductVO>> page(ProductQueryReq req) {
        PageResult<ProductVO> pageResult = productService.page(req);
        return Result.success(pageResult);
    }

    /**
     * 商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getDetail(@PathVariable Long id) {
        ProductDetailVO detail = productService.getDetailWithCache(id);
        return Result.success(detail);
    }
}
