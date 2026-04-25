package com.example.mall.module.product.controller;

import com.example.mall.auth.annotation.RequireRole;
import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.product.dto.ProductCreateReq;
import com.example.mall.module.product.dto.ProductQueryReq;
import com.example.mall.module.product.dto.ProductUpdateReq;
import com.example.mall.module.product.dto.ProductVO;
import com.example.mall.module.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商品管理端接口（需 OPERATOR 以上权限）
 *
 * <p>提供商品的增删改查、上下架及分页列表功能。
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@RequireRole
public class AdminProductController {

    private final ProductService productService;

    /**
     * 管理端分页列表
     */
    @GetMapping
    public Result<PageResult<ProductVO>> page(ProductQueryReq req) {
        PageResult<ProductVO> pageResult = productService.adminPage(req);
        return Result.success(pageResult);
    }

    /**
     * 创建商品
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ProductCreateReq req) {
        productService.create(req);
        return Result.success();
    }

    /**
     * 更新商品
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductUpdateReq req) {
        productService.update(req);
        return Result.success();
    }

    /**
     * 删除商品（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    /**
     * 上架/下架
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0（下架）或 1（上架）");
        }
        productService.updateStatus(id, status);
        return Result.success();
    }
}
