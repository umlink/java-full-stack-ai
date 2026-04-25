package com.example.mall.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.module.product.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 品牌 Mapper
 */
@Mapper
public interface BrandMapper extends BaseMapper<Brand> {

    /**
     * 根据品牌名称查询
     */
    @Select("SELECT * FROM brand WHERE name = #{name}")
    Brand selectByName(String name);
}
