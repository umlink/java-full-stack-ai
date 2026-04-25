package com.example.mall.module.product.controller;

import com.example.mall.common.Result;
import com.example.mall.module.product.dto.CategoryVO;
import com.example.mall.module.product.entity.Category;
import com.example.mall.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开分类接口（无需认证）
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类树形结构（仅启用的）
     */
    @GetMapping
    public Result<List<CategoryVO>> getCategoryTree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 获取单个分类
     */
    @GetMapping("/{id}")
    public Result<CategoryVO> getById(@PathVariable Long id) {
        // 转为 VO 返回，避免暴露敏感字段
        Category category = categoryService.getById(id);

        if (category.getStatus() != 1) {
            return Result.error(404, "分类不存在");
        }

        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setLevel(category.getLevel());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return Result.success(vo);
    }
}
