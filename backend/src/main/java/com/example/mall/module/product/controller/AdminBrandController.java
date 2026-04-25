package com.example.mall.module.product.controller;

import com.example.mall.auth.annotation.RequireRole;
import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.common.dto.PageParams;
import com.example.mall.module.product.entity.Brand;
import com.example.mall.module.product.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 品牌管理端接口（需 OPERATOR 以上权限）
 *
 * <p>提供品牌的增删改查及分页列表功能。
 */
@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
@RequireRole
public class AdminBrandController {

    private final BrandService brandService;

    /**
     * 创建品牌
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody Brand brand) {
        brandService.create(brand);
        return Result.success();
    }

    /**
     * 更新品牌
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Brand brand) {
        brandService.update(brand);
        return Result.success();
    }

    /**
     * 删除品牌（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.success();
    }

    /**
     * 分页获取品牌列表
     */
    @GetMapping
    public Result<PageResult<Brand>> page(PageParams params) {
        PageResult<Brand> pageResult = brandService.page(params);
        return Result.success(pageResult);
    }
}
