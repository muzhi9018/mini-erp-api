package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.muzhi.minierp.entity.product.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.vo.product.ProductVO;
import com.muzhi.minierp.vo.product.ProductQueryVO;

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

    /**
     * 锁定有效商品；所有主档修改与快照获取共用该锁。
     */
    Product selectForUpdate(@Param("id") Long id);

    /**
     * 编码包含已删除记录，不允许重新分配。
     */
    boolean existsByCode(@Param("code") String code);

    /**
     * 商品去重后分页，SKU 条件使用 EXISTS。
     */
    IPage<ProductVO> selectProductPage(IPage<ProductVO> page, @Param("query") ProductQueryVO query,
            @Param("categoryIds") List<Long> categoryIds);

    /**
     * 查询单位影响的商品，按 ID 升序锁定。
     */
    List<Long> selectIdsByUnit(@Param("unitId") Long unitId);
}
