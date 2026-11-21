package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductSkuSpecValue;

import com.muzhi.minierp.vo.product.ProductSkuSpecValueVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * <p>
 * SKU 规格选择关系，商品归属和维度选择完整性由应用事务保证 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductSkuSpecValueMapper extends BaseMapper<ProductSkuSpecValue> {

    /**
     * 批量查询 SKU 的结构化规格，按维度顺序返回。
     */
    List<ProductSkuSpecValueVO> selectDetails(@Param("skuIds") List<Long> skuIds);
}
