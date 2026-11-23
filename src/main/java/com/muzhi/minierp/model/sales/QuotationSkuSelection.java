package com.muzhi.minierp.model.sales;

import com.muzhi.minierp.vo.product.ProductOptionVO;

import java.util.List;

/**
 * 报价明细和模板共用的实时选品检查结果。
 *
 * @param referenceSaleCurrency 当前售价币种
 * @param option 当前选品资料
 * @param issues 不可用原因码
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationSkuSelection(String referenceSaleCurrency, ProductOptionVO option, List<String> issues) {
}
