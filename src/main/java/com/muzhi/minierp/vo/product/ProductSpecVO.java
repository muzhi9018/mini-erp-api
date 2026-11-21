package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductSpec;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 商品规格维度及候选值
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSpec.class)
public class ProductSpecVO extends ProductSpec {

    /**
     * 完整创建时的临时维度键，不持久化
     */
    private String clientKey;

    /**
     * 期望商品版本
     */
    private Long expectedVersion;

    /**
     * 候选值列表
     */
    private List<ProductSpecValueVO> values;
}
