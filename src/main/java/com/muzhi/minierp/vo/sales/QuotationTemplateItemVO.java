package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.QuotationTemplateItem;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 模板明细及实时选品检查结果。
 *
 * @author Mr.Muzhi
 * @since 2026-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = QuotationTemplateItem.class, convertGenerate = false)
public class QuotationTemplateItemVO extends QuotationTemplateItem {

    @Serial
    private static final long serialVersionUID = -5869750865497938699L;

    /**
     * 当前参考售价币种
     */
    private String referenceSaleCurrency;

    /**
     * SKU 当前选品资料
     */
    private ProductOptionVO product;

    /**
     * 当前选品不可用的原因码
     */
    private List<String> issues;
}
