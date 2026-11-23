package com.muzhi.minierp.vo.sales;

import java.util.List;

/**
 * 正式报价详情或复制草稿，包含编辑表单和客户版展示数据。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationDetailVO(
        Long id,
        Long version,
        Boolean orderGenerated,
        QuotationFormVO form,
        QuotationDocumentVO document,
        List<Long> skuIds
) {
}
