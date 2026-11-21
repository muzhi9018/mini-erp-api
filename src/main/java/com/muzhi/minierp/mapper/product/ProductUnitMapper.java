package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductUnit;

import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 商品计量单位，供 SKU 数量和数值规格使用，不提供单位换算 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductUnitMapper extends BaseMapper<ProductUnit> {


    /**
     * 单位编码不允许在删除后复用。
     */
    boolean existsByCode(@Param("code") String code);

    /**
     * 商品引用单位时加共享锁，防止并发删除或修改。
     */
    ProductUnit selectForShare(@Param("id") Long id);

    /**
     * 单位维护时在商品锁之后加排他锁。
     */
    ProductUnit selectForUpdate(@Param("id") Long id);
}
