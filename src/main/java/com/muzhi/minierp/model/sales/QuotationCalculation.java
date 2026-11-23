package com.muzhi.minierp.model.sales;

import com.muzhi.minierp.entity.sales.Quotation;

import java.util.List;

/**
 * 报价计价结果，包含整单合计和按表单顺序计算的明细。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationCalculation(Quotation header, List<QuotationLine> lines) {
}
