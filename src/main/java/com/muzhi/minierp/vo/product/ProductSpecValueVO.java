package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductSpecValue;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 商品规格候选值维护参数
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSpecValue.class)
public class ProductSpecValueVO extends ProductSpecValue {

    /**
     * 完整创建时的临时候选值键，不持久化
     */
    private String clientKey;

    /**
     * 期望商品版本
     */
    private Long expectedVersion;
}
