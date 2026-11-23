package com.muzhi.minierp.vo.sales;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预览和历史详情共用的客户版展示数据。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationDocumentVO(
        String quoteNumber,
        String title,
        OffsetDateTime quotedAt,
        String currency,
        Issuer issuer,
        QuotationCustomerVO customer,
        String remark,
        QuotationFormVO.Discount discount,
        Totals totals,
        List<Item> items
) {

    /**
     * 开单方名称和联系方式随历史报价保留，Logo 使用现行配置。
     */
    public record Issuer(String name, String contact, String logoUrl) {
    }

    /**
     * SKU 描述、数量及成交数据。
     */
    public record Item(
            String skuCode,
            String productName,
            String model,
            String specificationText,
            String unitName,
            Integer quantityScale,
            String quantity,
            String unitPrice,
            QuotationFormVO.Discount discount,
            String originalAmount,
            String discountAmount,
            String lineAmount,
            String remark
    ) {
    }

    /**
     * 各行原价、商品优惠、整单优惠与最终报价金额。
     */
    public record Totals(
            String originalAmount, String lineDiscountAmount, String subtotalAmount,
            String orderDiscountAmount, String totalAmount
    ) {
    }
}
