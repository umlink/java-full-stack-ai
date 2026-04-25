package com.example.mall.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.module.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品SKU Mapper
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 根据商品ID查询SKU列表（按排序值升序）
     */
    @Select("SELECT * FROM product_sku WHERE product_id = #{productId} ORDER BY sort_order ASC")
    List<ProductSku> selectByProductId(Long productId);

    /**
     * 带行锁查询 SKU
     */
    @Select("SELECT * FROM product_sku WHERE id = #{id} FOR UPDATE")
    ProductSku selectByIdForUpdate(@Param("id") Long id);
}
