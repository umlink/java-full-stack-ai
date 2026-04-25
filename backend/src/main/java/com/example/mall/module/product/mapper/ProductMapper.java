package com.example.mall.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mall.module.product.dto.ProductQueryReq;
import com.example.mall.module.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 带条件的分页查询（支持分类、品牌、价格区间、关键词搜索、排序）
     */
    IPage<Product> selectPageWithCondition(IPage<Product> page, @Param("req") ProductQueryReq req);

    /**
     * 带行锁查询商品
     */
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product selectByIdForUpdate(@Param("id") Long id);
}
