package com.example.mall.module.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BusinessException;
import com.example.mall.common.PageResult;
import com.example.mall.common.dto.PageParams;
import com.example.mall.module.product.entity.Brand;
import com.example.mall.module.product.mapper.BrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 品牌业务逻辑
 */
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandMapper brandMapper;

    /**
     * 查询所有启用的品牌（按排序值升序）
     */
    public List<Brand> getActiveBrands() {
        return brandMapper.selectList(
                Wrappers.<Brand>lambdaQuery()
                        .eq(Brand::getStatus, 1)
                        .orderByAsc(Brand::getSortOrder)
        );
    }

    /**
     * 管理端分页列表
     */
    public PageResult<Brand> page(PageParams params) {
        Page<Brand> page = new Page<>(params.getPage(), params.getPageSize());
        Page<Brand> result = brandMapper.selectPage(page,
                Wrappers.<Brand>lambdaQuery().orderByAsc(Brand::getSortOrder));
        return PageResult.of(result);
    }

    /**
     * 根据 ID 查询品牌
     */
    public Brand getById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(20004, "品牌不存在");
        }
        return brand;
    }

    /**
     * 创建品牌（校验名称唯一性）
     */
    @Transactional
    public void create(Brand brand) {
        // 校验品牌名唯一性
        Brand existing = brandMapper.selectByName(brand.getName());
        if (existing != null) {
            throw new BusinessException(20005, "品牌名称已存在");
        }

        // 设置默认值
        if (brand.getStatus() == null) {
            brand.setStatus(1);
        }
        if (brand.getSortOrder() == null) {
            brand.setSortOrder(0);
        }

        brandMapper.insert(brand);
    }

    /**
     * 更新品牌（校验名称唯一性，排除自身）
     */
    @Transactional
    public void update(Brand brand) {
        // 校验品牌存在
        Brand existing = brandMapper.selectById(brand.getId());
        if (existing == null) {
            throw new BusinessException(20004, "品牌不存在");
        }

        // 如果修改了名称，校验名称唯一性（排除自身）
        if (brand.getName() != null && !brand.getName().equals(existing.getName())) {
            Brand nameConflict = brandMapper.selectByName(brand.getName());
            if (nameConflict != null) {
                throw new BusinessException(20005, "品牌名称已存在");
            }
        }

        brandMapper.updateById(brand);
    }

    /**
     * 删除品牌（软删除：设置 status = 0）
     */
    @Transactional
    public void delete(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(20004, "品牌不存在");
        }

        brand.setStatus(0);
        brandMapper.updateById(brand);
    }
}
