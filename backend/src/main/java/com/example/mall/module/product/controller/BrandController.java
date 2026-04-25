package com.example.mall.module.product.controller;

import com.example.mall.common.Result;
import com.example.mall.module.product.entity.Brand;
import com.example.mall.module.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 品牌公开接口（无需认证）
 *
 * <p>提供品牌列表和详情查询，供前端商品浏览等场景使用。
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * 获取全部启用的品牌列表（按排序值升序）
     */
    @GetMapping
    public Result<List<Brand>> getActiveBrands() {
        List<Brand> brands = brandService.getActiveBrands();
        return Result.success(brands);
    }

    /**
     * 根据 ID 获取品牌详情
     */
    @GetMapping("/{id}")
    public Result<Brand> getById(@PathVariable Long id) {
        return Result.success(brandService.getById(id));
    }
}
