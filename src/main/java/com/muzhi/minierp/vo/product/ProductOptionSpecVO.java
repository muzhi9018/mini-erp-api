package com.muzhi.minierp.vo.product;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.math.BigDecimal;

/**
 * <p>
 * 选品规格，不包含管理审计信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@AutoMapper(target = ProductSkuSpecValueVO.class)
public class ProductOptionSpecVO {

    /**
     * 维度 ID
     */
    private Long specId;

    /**
     * 候选值 ID
     */
    private Long specValueId;

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
     * 单位名称
     */
    private String unitName;

}
