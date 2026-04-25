package com.example.mall.module.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.module.cart.dto.AddCartReq;
import com.example.mall.module.cart.dto.CartVO;
import com.example.mall.module.cart.entity.CartItem;
import com.example.mall.module.cart.mapper.CartMapper;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.entity.ProductSku;
import com.example.mall.module.product.mapper.ProductMapper;
import com.example.mall.module.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 购物车业务逻辑
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    // ========== 购物车最大数量限制 ==========

    private static final int MAX_QUANTITY = 99;

    /**
     * 获取用户购物车列表
     */
    public List<CartVO> getMyCart(Long userId) {
        return cartMapper.selectCartWithProduct(userId);
    }

    /**
     * 添加商品到购物车
     *
     * <p>流程：
     * <ol>
     *   <li>校验商品存在且已上架</li>
     *   <li>多规格商品必须有 skuId</li>
     *   <li>校验 SKU/商品库存 &gt; 0</li>
     *   <li>检查是否已存在相同 (user_id, product_id, sku_id) 记录</li>
     *   <li>存在则 UPDATE quantity，否则 INSERT</li>
     *   <li>校验总数量 &lt;= 99</li>
     * </ol>
     */
    public void addItem(Long userId, AddCartReq req) {
        // 1. 校验商品存在且已上架
        Product product = productMapper.selectById(req.getProductId());
        if (product == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != 1) {
            throw new BusinessException(BizCode.PRODUCT_OFFLINE);
        }

        // 2. 多规格商品必须有 skuId
        boolean hasSku = Boolean.TRUE.equals(product.getHasSku());
        if (hasSku && req.getSkuId() == null) {
            throw new BusinessException(BizCode.CART_SKU_REQUIRED);
        }

        // 3. 校验库存
        if (hasSku) {
            ProductSku sku = productSkuMapper.selectById(req.getSkuId());
            if (sku == null) {
                throw new BusinessException(BizCode.PRODUCT_NOT_FOUND.getCode(), "SKU不存在");
            }
            if (sku.getStock() <= 0) {
                throw new BusinessException(BizCode.STOCK_NOT_ENOUGH);
            }
        } else {
            if (product.getTotalStock() == null || product.getTotalStock() <= 0) {
                throw new BusinessException(BizCode.STOCK_NOT_ENOUGH);
            }
        }

        // 4. 查询是否已存在相同记录
        CartItem existing = cartMapper.selectByUserAndProduct(userId, req.getProductId(), req.getSkuId());

        if (existing != null) {
            // 5. 已存在 → 累加数量
            int newQuantity = existing.getQuantity() + req.getQuantity();
            if (newQuantity > MAX_QUANTITY) {
                throw new BusinessException(BizCode.CART_QUANTITY_EXCEED);
            }
            existing.setQuantity(newQuantity);
            cartMapper.updateById(existing);
        } else {
            // 6. 不存在 → 新增
            if (req.getQuantity() > MAX_QUANTITY) {
                throw new BusinessException(BizCode.CART_QUANTITY_EXCEED);
            }
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(req.getProductId());
            cartItem.setSkuId(req.getSkuId());
            cartItem.setQuantity(req.getQuantity());
            cartItem.setSelected(true);
            cartMapper.insert(cartItem);
        }
    }

    /**
     * 修改购物车项数量
     */
    public void updateQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessException(BizCode.CART_QUANTITY_EXCEED);
        }

        CartItem cartItem = cartMapper.selectById(itemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.CART_NOT_FOUND);
        }

        cartItem.setQuantity(quantity);
        cartMapper.updateById(cartItem);
    }

    /**
     * 删除购物车项
     */
    public void deleteItem(Long userId, Long itemId) {
        CartItem cartItem = cartMapper.selectById(itemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(BizCode.CART_NOT_FOUND);
        }

        cartMapper.deleteById(itemId);
    }

    /**
     * 清空已选商品（下单后调用）
     */
    public void clearSelected(Long userId) {
        cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSelected, true));
    }
}
