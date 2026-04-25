package com.example.mall.module.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.module.cart.dto.CartVO;
import com.example.mall.module.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartMapper extends BaseMapper<CartItem> {

    /**
     * 查询用户购物车列表（关联商品和 SKU 信息）
     */
    List<CartVO> selectCartWithProduct(@Param("userId") Long userId);

    /**
     * 统计用户购物车中相同(user_id, product_id, sku_id)的记录数量
     */
    int countByUserAndProduct(@Param("userId") Long userId,
                              @Param("productId") Long productId,
                              @Param("skuId") Long skuId);

    /**
     * 查询已存在的购物车项（用于加购时判断）
     */
    CartItem selectByUserAndProduct(@Param("userId") Long userId,
                                    @Param("productId") Long productId,
                                    @Param("skuId") Long skuId);
}
