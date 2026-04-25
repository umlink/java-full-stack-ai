package com.example.mall.module.product.controller;

import com.example.mall.auth.annotation.RequireRole;
import com.example.mall.common.PageParams;
import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.product.entity.Category;
import com.example.mall.module.product.service.CategoryService;
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
 * 管理端分类接口（需 OPERATOR 以上权限）
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@RequireRole
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * 分页查询分类列表（含禁用的）
     */
    @GetMapping
    public Result<PageResult<Category>> list(@Valid PageParams params) {
        PageResult<Category> page = categoryService.list(params);
        return Result.success(page);
    }

    /**
     * 创建分类
     */
    @PostMapping
    public Result<Void> create(@RequestBody Category category) {
        categoryService.create(category);
        return Result.success();
    }

    /**
     * 更新分类
     */
    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success();
    }

    /**
     * 删除分类（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
