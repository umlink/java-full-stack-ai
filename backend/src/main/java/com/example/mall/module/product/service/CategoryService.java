package com.example.mall.module.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.common.PageParams;
import com.example.mall.common.PageResult;
import com.example.mall.module.product.dto.CategoryVO;
import com.example.mall.module.product.entity.Category;
import com.example.mall.module.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类业务逻辑
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    // ========== 公开接口 ==========

    /**
     * 查询所有启用的分类，组装为树形结构
     */
    public List<CategoryVO> getCategoryTree() {
        // 查询所有启用的分类，按 sort_order 升序排列
        List<Category> all = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
        );

        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        return buildTree(all);
    }

    // ========== 管理端接口 ==========

    /**
     * 分页查询分类列表（含禁用的）
     */
    public PageResult<Category> list(PageParams params) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Category> page =
                categoryMapper.selectPage(
                        params.toMyBatisPlusPage(),
                        Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSortOrder)
                );
        return PageResult.of(page);
    }

    /**
     * 根据 ID 获取分类
     */
    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        return category;
    }

    /**
     * 创建分类
     */
    @Transactional
    public void create(Category category) {
        // 如果指定了 parentId，校验父分类是否存在
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent == null) {
                throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "父分类不存在");
            }
            // 子分类 level = 父分类 level + 1
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParentId(0L);
            category.setLevel(1);
        }

        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        categoryMapper.insert(category);
    }

    /**
     * 更新分类
     */
    @Transactional
    public void update(Category category) {
        Category existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "分类不存在");
        }

        // 校验父分类不能是自己
        if (category.getParentId() != null && category.getParentId().equals(category.getId())) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "父分类不能是自己");
        }

        // 如果更新了 parentId，重新计算 level
        if (category.getParentId() != null && !category.getParentId().equals(existing.getParentId())) {
            if (category.getParentId() > 0) {
                Category parent = categoryMapper.selectById(category.getParentId());
                if (parent == null) {
                    throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "父分类不存在");
                }
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setParentId(0L);
                category.setLevel(1);
            }
        }

        categoryMapper.updateById(category);
    }

    /**
     * 删除分类（软删除）
     */
    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "分类不存在");
        }

        // 检查是否有子分类
        Long childCount = categoryMapper.selectCount(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "该分类下有子分类，无法删除");
        }

        // 软删除：将 status 置为 0
        category.setStatus(0);
        categoryMapper.updateById(category);
    }

    // ========== 内部辅助方法 ==========

    /**
     * 将扁平分类列表构建为树形结构
     */
    private List<CategoryVO> buildTree(List<Category> all) {
        // 转换为 VO 列表
        List<CategoryVO> voList = all.stream()
                .map(this::toCategoryVO)
                .collect(Collectors.toList());

        // 按 parentId 分组
        Map<Long, List<CategoryVO>> parentIdMap = voList.stream()
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        // 为每个分类设置 children
        for (CategoryVO vo : voList) {
            List<CategoryVO> children = parentIdMap.getOrDefault(vo.getId(), Collections.emptyList());
            if (!children.isEmpty()) {
                // 子分类已按 sort_order 排序，这里保持顺序
                vo.setChildren(children);
            }
        }

        // 返回顶层分类（parentId = 0）
        return parentIdMap.getOrDefault(0L, Collections.emptyList());
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setLevel(category.getLevel());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
