package com.example.mall.module.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.common.PageResult;
import com.example.mall.common.ProductBloomFilter;
import com.example.mall.module.product.dto.ProductCreateReq;
import com.example.mall.module.product.dto.ProductDetailVO;
import com.example.mall.module.product.dto.ProductQueryReq;
import com.example.mall.module.product.dto.ProductUpdateReq;
import com.example.mall.module.product.dto.ProductVO;
import com.example.mall.module.product.entity.Brand;
import com.example.mall.module.product.entity.Category;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.entity.ProductSku;
import com.example.mall.module.product.mapper.ProductMapper;
import com.example.mall.module.product.mapper.ProductSkuMapper;
import com.example.mall.module.product.service.BrandService;
import com.example.mall.module.product.service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ProductBloomFilter productBloomFilter;

    // ========== 公开接口 ==========

    /**
     * 商品分页列表（含条件筛选/搜索/排序）
     * 公开接口仅返回已上架商品
     */
    public PageResult<ProductVO> page(ProductQueryReq req) {
        // 公开接口仅显示已上架商品
        if (req.getStatus() == null) {
            req.setStatus(1);
        }

        Page<Product> page = new Page<>(req.getPage(), req.getPageSize());
        IPage<Product> result = productMapper.selectPageWithCondition(page, req);

        List<ProductVO> voList = result.getRecords().stream()
                .map(this::toProductVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 商品详情（含 SKU 列表 + 品牌名 + 分类名）
     */
    public ProductDetailVO getDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != 1) {
            throw new BusinessException(BizCode.PRODUCT_OFFLINE);
        }
        return toProductDetailVO(product);
    }

    // ========== 缓存策略 ==========

    /**
     * 查询商品详情（带缓存）
     *
     * <p>缓存穿透防护：布隆过滤器 + 空值缓存<br>
     * 缓存击穿防护：互斥锁 + 双重检查<br>
     * 缓存雪崩防护：TTL 随机化
     */
    public ProductDetailVO getDetailWithCache(Long id) {
        String cacheKey = "product:" + id;

        // ① 布隆过滤器检查（缓存穿透防护）
        if (!productBloomFilter.mightContain(cacheKey)) {
            return null;
        }

        // ② 读缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductDetailVO.class);
            } catch (Exception e) {
                // 缓存数据异常，回源到 DB
            }
        }

        // 空值缓存检查（缓存穿透防护）
        String nullKey = cacheKey + ":null";
        Boolean hasNull = stringRedisTemplate.hasKey(nullKey);
        if (Boolean.TRUE.equals(hasNull)) {
            return null;
        }

        // ③ 互斥锁加载（防止缓存击穿）
        RLock lock = redissonClient.getLock("cache:mutex:" + cacheKey);
        try {
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查
                    cached = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        try {
                            return objectMapper.readValue(cached, ProductDetailVO.class);
                        } catch (Exception e) {
                            // 缓存数据异常，回源到 DB
                        }
                    }

                    // 查 DB
                    Product product = productMapper.selectById(id);
                    if (product != null && product.getStatus() == 1) {
                        ProductDetailVO detail = toProductDetailVO(product);
                        // 写入缓存，TTL 随机化防雪崩
                        int ttl = 3600 + ThreadLocalRandom.current().nextInt(600);
                        stringRedisTemplate.opsForValue().set(cacheKey, toJsonString(detail), ttl, TimeUnit.SECONDS);
                        return detail;
                    } else {
                        // 空值缓存（防止缓存穿透）
                        stringRedisTemplate.opsForValue().set(nullKey, "1", 60, TimeUnit.SECONDS);
                        return null;
                    }
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 中断时降级到直接查 DB
            Product product = productMapper.selectById(id);
            if (product == null || product.getStatus() != 1) {
                return null;
            }
            return toProductDetailVO(product);
        }

        // 获取锁失败，短暂等待后重试
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Product product = productMapper.selectById(id);
            if (product == null || product.getStatus() != 1) {
                return null;
            }
            return toProductDetailVO(product);
        }
        return getDetailWithCache(id);
    }

    /**
     * 商品分页列表（带缓存）
     *
     * <p>关键词搜索不缓存；只缓存前 5 页；TTL 随机化防雪崩。
     */
    public PageResult<ProductVO> pageWithCache(ProductQueryReq req) {
        // 关键词搜索不缓存
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            return page(req);
        }

        // 只缓存前 5 页
        if (req.getPage() > 5) {
            return page(req);
        }

        // 构建缓存 key
        String cacheKey = buildListCacheKey(req);

        // 读缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<PageResult<ProductVO>>() {});
            } catch (Exception e) {
                // 缓存数据异常，回源到 DB
            }
        }

        // 查 DB
        PageResult<ProductVO> result = page(req);

        // 写缓存（TTL 随机化）
        int ttl = 600 + ThreadLocalRandom.current().nextInt(120);
        stringRedisTemplate.opsForValue().set(cacheKey, toJsonString(result), ttl, TimeUnit.SECONDS);

        return result;
    }

    // ========== 缓存失效 ==========

    /**
     * 删除商品详情缓存（含空值缓存）
     */
    private void evictProductCache(Long productId) {
        stringRedisTemplate.delete("product:" + productId);
        stringRedisTemplate.delete("product:" + productId + ":null");
    }

    /**
     * 删除商品列表缓存（使用 pattern 匹配）
     */
    private void evictListCache() {
        Set<String> keys = stringRedisTemplate.keys("product:list:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 构建列表缓存 key（包含分类/品牌/排序/价格区间/分页参数）
     */
    private String buildListCacheKey(ProductQueryReq req) {
        StringBuilder sb = new StringBuilder("product:list:");
        sb.append(req.getPage()).append(":").append(req.getPageSize()).append(":");
        sb.append(req.getCategoryId() != null ? req.getCategoryId() : "").append(":");
        sb.append(req.getBrandId() != null ? req.getBrandId() : "").append(":");
        sb.append(req.getSort() != null ? req.getSort() : "").append(":");
        sb.append(req.getMinPrice() != null ? req.getMinPrice() : "").append(":");
        sb.append(req.getMaxPrice() != null ? req.getMaxPrice() : "");
        return sb.toString();
    }

    // ========== 管理端接口 ==========

    /**
     * 管理端分页列表
     */
    public PageResult<ProductVO> adminPage(ProductQueryReq req) {
        Page<Product> page = new Page<>(req.getPage(), req.getPageSize());
        IPage<Product> result = productMapper.selectPageWithCondition(page, req);

        List<ProductVO> voList = result.getRecords().stream()
                .map(this::toProductVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 创建商品（支持多规格同时创建 SKU）
     */
    @Transactional
    public void create(ProductCreateReq req) {
        // 校验分类存在
        Category category = categoryService.getById(req.getCategoryId());
        if (category == null || category.getStatus() != 1) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "分类不存在或已禁用");
        }

        Product product = new Product();
        product.setName(req.getName());
        product.setBrief(req.getBrief());
        product.setDescription(req.getDescription());
        product.setCategoryId(req.getCategoryId());
        product.setBrandId(req.getBrandId());
        product.setUnit(req.getUnit() != null ? req.getUnit() : "件");
        product.setWeight(req.getWeight());
        product.setHasSku(req.getHasSku() != null ? req.getHasSku() : false);
        product.setMainImage(req.getMainImage());
        product.setImages(toJsonString(req.getImages()));
        product.setVideoUrl(req.getVideoUrl());
        product.setSpecs(toJsonString(req.getSpecs()));
        product.setAttrs(toJsonString(req.getAttrs()));
        product.setTags(toJsonString(req.getTags()));
        product.setKeywords(req.getKeywords());
        product.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        product.setStatus(1); // 默认上架
        product.setSales(0);

        if (Boolean.TRUE.equals(req.getHasSku())) {
            // 多规格：由 SKU 聚合计算
            if (req.getSkus() == null || req.getSkus().isEmpty()) {
                throw new BusinessException(20006, "多规格商品必须至少有一个SKU");
            }
            product.setTotalStock(0);
            product.setPrice(BigDecimal.ZERO);
            product.setMinPrice(null);
            product.setMaxPrice(null);
        } else {
            // 单规格：直接使用传入值
            product.setPrice(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO);
            product.setTotalStock(req.getTotalStock() != null ? req.getTotalStock() : 0);
        }

        productMapper.insert(product);

        // 加入布隆过滤器
        productBloomFilter.put("product:" + product.getId());

        // 多规格时创建 SKU 并聚合更新
        if (Boolean.TRUE.equals(req.getHasSku()) && req.getSkus() != null && !req.getSkus().isEmpty()) {
            processSkuCreation(product.getId(), req.getSkus());
        }

        // 失效列表缓存
        evictListCache();
    }

    /**
     * 更新商品
     */
    @Transactional
    public void update(ProductUpdateReq req) {
        Product existing = productMapper.selectById(req.getId());
        if (existing == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }

        Product product = new Product();
        product.setId(req.getId());

        if (req.getName() != null) product.setName(req.getName());
        if (req.getBrief() != null) product.setBrief(req.getBrief());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getCategoryId() != null) product.setCategoryId(req.getCategoryId());
        if (req.getBrandId() != null) product.setBrandId(req.getBrandId());
        if (req.getUnit() != null) product.setUnit(req.getUnit());
        if (req.getWeight() != null) product.setWeight(req.getWeight());
        if (req.getHasSku() != null) product.setHasSku(req.getHasSku());
        if (req.getMainImage() != null) product.setMainImage(req.getMainImage());
        if (req.getImages() != null) product.setImages(toJsonString(req.getImages()));
        if (req.getVideoUrl() != null) product.setVideoUrl(req.getVideoUrl());
        if (req.getSpecs() != null) product.setSpecs(toJsonString(req.getSpecs()));
        if (req.getAttrs() != null) product.setAttrs(toJsonString(req.getAttrs()));
        if (req.getTags() != null) product.setTags(toJsonString(req.getTags()));
        if (req.getKeywords() != null) product.setKeywords(req.getKeywords());
        if (req.getSortOrder() != null) product.setSortOrder(req.getSortOrder());

        // 判断是否切换规格模式
        boolean isMultiSku = req.getHasSku() != null ? req.getHasSku() : Boolean.TRUE.equals(existing.getHasSku());

        if (!isMultiSku) {
            // 单规格：直接使用传入值
            if (req.getPrice() != null) product.setPrice(req.getPrice());
            if (req.getTotalStock() != null) product.setTotalStock(req.getTotalStock());
        }

        productMapper.updateById(product);

        // 多规格时更新 SKU
        if (isMultiSku && req.getSkus() != null) {
            processSkuUpdate(req.getId(), req.getSkus());
        }

        // 失效缓存
        evictProductCache(req.getId());
        evictListCache();
    }

    /**
     * 上架/下架
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(status);
        productMapper.updateById(product);
        // 失效缓存
        evictProductCache(id);
        evictListCache();
    }

    /**
     * 软删除（设置 status = 0）
     */
    @Transactional
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(0);
        productMapper.updateById(product);
        // 失效缓存
        evictProductCache(id);
        evictListCache();
    }

    // ========== SKU 处理逻辑 ==========

    /**
     * 处理多规格 SKU 创建
     */
    private void processSkuCreation(Long productId, List<ProductCreateReq.SkuItem> skuItems) {
        List<BigDecimal> skuPrices = new ArrayList<>();
        int totalStock = 0;

        for (ProductCreateReq.SkuItem skuItem : skuItems) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setName(skuItem.getName());
            sku.setAttrs(toJsonString(skuItem.getAttrs()));
            sku.setPrice(skuItem.getPrice());
            sku.setStock(skuItem.getStock());
            sku.setCode(skuItem.getCode());
            sku.setImage(skuItem.getImage());
            sku.setWeight(skuItem.getWeight());
            sku.setStatus(1);
            sku.setSortOrder(skuItem.getSortOrder() != null ? skuItem.getSortOrder() : 0);
            productSkuMapper.insert(sku);

            skuPrices.add(skuItem.getPrice());
            totalStock += skuItem.getStock();
        }

        // 聚合更新商品表
        Product update = new Product();
        update.setId(productId);
        BigDecimal minPrice = skuPrices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = skuPrices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        update.setPrice(minPrice);
        update.setMinPrice(minPrice);
        update.setMaxPrice(maxPrice);
        update.setTotalStock(totalStock);
        productMapper.updateById(update);
    }

    /**
     * 处理多规格 SKU 更新
     */
    private void processSkuUpdate(Long productId, List<ProductUpdateReq.SkuItem> skuItems) {
        // 收集需要保留的 SKU ID
        Set<Long> updateSkuIds = skuItems.stream()
                .map(ProductUpdateReq.SkuItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 删除不在更新列表中的旧 SKU
        if (!updateSkuIds.isEmpty()) {
            productSkuMapper.delete(Wrappers.<ProductSku>lambdaQuery()
                    .eq(ProductSku::getProductId, productId)
                    .notIn(ProductSku::getId, updateSkuIds));
        } else {
            // 没有保留的 SKU ID，删除所有旧 SKU
            productSkuMapper.delete(Wrappers.<ProductSku>lambdaQuery()
                    .eq(ProductSku::getProductId, productId));
        }

        List<BigDecimal> skuPrices = new ArrayList<>();
        int totalStock = 0;

        for (ProductUpdateReq.SkuItem skuItem : skuItems) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setName(skuItem.getName());
            sku.setAttrs(toJsonString(skuItem.getAttrs()));
            sku.setPrice(skuItem.getPrice());
            sku.setStock(skuItem.getStock());
            sku.setCode(skuItem.getCode());
            sku.setImage(skuItem.getImage());
            sku.setWeight(skuItem.getWeight());
            sku.setStatus(skuItem.getStatus() != null ? skuItem.getStatus() : 1);
            sku.setSortOrder(skuItem.getSortOrder() != null ? skuItem.getSortOrder() : 0);

            if (skuItem.getId() != null) {
                sku.setId(skuItem.getId());
                productSkuMapper.updateById(sku);
            } else {
                productSkuMapper.insert(sku);
            }

            skuPrices.add(skuItem.getPrice());
            totalStock += skuItem.getStock();
        }

        // 聚合更新商品表
        Product update = new Product();
        update.setId(productId);
        BigDecimal minPrice = skuPrices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = skuPrices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        update.setPrice(minPrice);
        update.setMinPrice(minPrice);
        update.setMaxPrice(maxPrice);
        update.setTotalStock(totalStock);
        productMapper.updateById(update);
    }

    // ========== VO 转换 ==========

    /**
     * 转换为列表展示 VO
     */
    private ProductVO toProductVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setBrief(product.getBrief());
        vo.setCategoryId(product.getCategoryId());
        vo.setBrandId(product.getBrandId());
        vo.setUnit(product.getUnit());
        vo.setHasSku(product.getHasSku());
        vo.setPrice(product.getPrice());
        vo.setMinPrice(product.getMinPrice());
        vo.setMaxPrice(product.getMaxPrice());
        vo.setTotalStock(product.getTotalStock());
        vo.setSales(product.getSales());
        vo.setMainImage(product.getMainImage());
        vo.setImages(parseJsonList(product.getImages()));
        vo.setTags(parseJsonList(product.getTags()));
        vo.setKeywords(product.getKeywords());
        vo.setSortOrder(product.getSortOrder());
        vo.setStatus(product.getStatus());
        vo.setCreatedAt(product.getCreatedAt());

        // 查询分类名称
        if (product.getCategoryId() != null) {
            try {
                Category category = categoryService.getById(product.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            } catch (Exception e) {
                // 分类可能已被删除，忽略
            }
        }

        // 查询品牌名称
        if (product.getBrandId() != null) {
            try {
                Brand brand = brandService.getById(product.getBrandId());
                if (brand != null) {
                    vo.setBrandName(brand.getName());
                }
            } catch (Exception e) {
                // 品牌可能已被删除，忽略
            }
        }

        return vo;
    }

    /**
     * 转换为详情 VO
     */
    private ProductDetailVO toProductDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setBrief(product.getBrief());
        vo.setDescription(product.getDescription());
        vo.setCategoryId(product.getCategoryId());
        vo.setBrandId(product.getBrandId());
        vo.setUnit(product.getUnit());
        vo.setWeight(product.getWeight());
        vo.setHasSku(product.getHasSku());
        vo.setPrice(product.getPrice());
        vo.setMinPrice(product.getMinPrice());
        vo.setMaxPrice(product.getMaxPrice());
        vo.setTotalStock(product.getTotalStock());
        vo.setSales(product.getSales());
        vo.setMainImage(product.getMainImage());
        vo.setImages(parseJsonList(product.getImages()));
        vo.setVideoUrl(product.getVideoUrl());
        vo.setSpecs(parseJsonListMap(product.getSpecs()));
        vo.setAttrs(parseJsonListMap(product.getAttrs()));
        vo.setTags(parseJsonList(product.getTags()));
        vo.setKeywords(product.getKeywords());
        vo.setSortOrder(product.getSortOrder());
        vo.setStatus(product.getStatus());
        vo.setCreatedAt(product.getCreatedAt());
        vo.setUpdatedAt(product.getUpdatedAt());

        // 查询分类名称
        if (product.getCategoryId() != null) {
            try {
                Category category = categoryService.getById(product.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            } catch (Exception e) {
                // 分类可能已被删除，忽略
            }
        }

        // 查询品牌名称
        if (product.getBrandId() != null) {
            try {
                Brand brand = brandService.getById(product.getBrandId());
                if (brand != null) {
                    vo.setBrandName(brand.getName());
                }
            } catch (Exception e) {
                // 品牌可能已被删除，忽略
            }
        }

        // 查询 SKU 列表
        if (Boolean.TRUE.equals(product.getHasSku())) {
            List<ProductSku> skus = productSkuMapper.selectByProductId(product.getId());
            vo.setSkus(skus);
        } else {
            vo.setSkus(Collections.emptyList());
        }

        return vo;
    }

    // ========== JSON 序列化/反序列化 ==========

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "JSON序列化失败");
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> parseJsonListMap(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

}
