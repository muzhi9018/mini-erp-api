package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductSkuSpecValue;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * <p>
 * SKU 规格选择及展示信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSkuSpecValue.class)
public class ProductSkuSpecValueVO extends ProductSkuSpecValue {

    /**
     * 维度名称
     */
    private String specName;

    /**
     * 值类型，见 {@link com.muzhi.minierp.enums.ProductEnum.SpecValueType}
     */
    private String valueType;

    /**
     * 文本值
     */
    private String textValue;

    /**
     * 数值
     */
    private BigDecimal numberValue;

    /**
     * 规格单位名称
     */
    private String unitName;
}
