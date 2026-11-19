package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 内部商品类目 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    /**
     * 包含逻辑删除记录的编码占用检查。
     */
    boolean existsByCode(@Param("code") String code);

    /**
     * 查询当前类目及全部有效后代的 ID。
     */
    List<Long> selectSubtreeIds(@Param("id") Long id);
}
