package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.enums.CurrencyEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报价选品分页与商品行刷新条件。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQuotationQueryVO extends ProductQueryVO {

    /**
     * 待刷新的 SKU ID。
     */
    private Long skuId;

    /**
     * 参考售价币种，见 {@link CurrencyEnum}。
     */
    private String referenceSaleCurrency;
}
