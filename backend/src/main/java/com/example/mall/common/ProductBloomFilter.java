package com.example.mall.common;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.mall.module.product.entity.Product;
import com.example.mall.module.product.mapper.ProductMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

/**
 * 商品布隆过滤器，用于缓存穿透防护。
 *
 * <p>在查询商品详情缓存前，先判断商品 ID 是否可能存在，避免无效请求直接打到数据库。
 * 商品创建时向布隆过滤器添加 ID。
 */
@Component
@RequiredArgsConstructor
public class ProductBloomFilter {

    private static final String BLOOM_FILTER_KEY = "product:bloom:ids";
    private static final long EXPECTED_INSERTIONS = 100_000;
    private static final double FALSE_PROBABILITY = 0.01;

    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_KEY, new StringCodec());
        boolean isNew = !bloomFilter.isExists();
        if (isNew) {
            bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        }

        // 启动时加载已有商品，确保种子数据和新创建的商品都能通过布隆过滤器
        // 每次启动都重新加载，防止因重建 Redis 导致的遗漏
        try {
            var ids = productMapper.selectObjs(
                    Wrappers.<Product>lambdaQuery().select(Product::getId)
            );
            if (ids != null) {
                for (Object id : ids) {
                    if (id != null) {
                        bloomFilter.add("product:" + id);
                    }
                }
            }
        } catch (Exception ignored) {
            // 首次启动时 productMapper 可能未就绪，忽略即可
        }
    }

    /**
     * 判断 key 是否可能存在（Redisson 的 RBloomFilter 使用 contains）
     */
    public boolean mightContain(String key) {
        return bloomFilter.contains(key);
    }

    /**
     * 向布隆过滤器添加 key
     */
    public void put(String key) {
        bloomFilter.add(key);
    }
}
