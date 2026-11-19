package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.muzhi.minierp.entity.product.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 内部商品档案，与官网商品独立维护 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 递增指定类目下有效商品的统一版本，审计字段沿用自动填充。
     */
    int incrementVersionsByCategoryIds(@Param("categoryIds") List<Long> categoryIds, @Param(Constants.ENTITY) Product product);
}
