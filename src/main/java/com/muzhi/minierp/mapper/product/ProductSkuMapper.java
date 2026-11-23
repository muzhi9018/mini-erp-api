package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductSku;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.vo.product.ProductSkuVO;
import com.muzhi.minierp.vo.product.ProductQueryVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * <p>
 * 内部商品 SKU，库存为人工维护的当前数量，不自动增减或预占 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductSkuMapper extends BaseMapper<ProductSku> {


    /**
     * 锁定 SKU，调用前必须先锁所属商品。
     */
    ProductSku selectForUpdate(@Param("id") Long id);

    /**
     * 编码包含已删除记录。
     */
    boolean existsByCode(@Param("code") String code);

    /**
     * 批量加载选品展示资料，保留停用项及未定价项供模板诊断。
     */
    List<ProductSkuVO> selectOptionsByIds(@Param("skuIds") List<Long> skuIds);

    /**
     * 按结构化规格、类目及关键字筛选，选品强制启用状态。
     */
    IPage<ProductSkuVO> selectSkuPage(IPage<ProductSkuVO> page, @Param("query") ProductQueryVO query,
            @Param("categoryIds") List<Long> categoryIds, @Param("options") boolean options,
            @Param("quotation") boolean quotation, @Param("referenceSaleCurrency") String referenceSaleCurrency);
}
