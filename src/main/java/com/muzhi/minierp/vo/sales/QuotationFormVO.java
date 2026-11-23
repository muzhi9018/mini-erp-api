package com.muzhi.minierp.vo.sales;

import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.JSONWriter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 新建、复制或修改报价共用表单。
 *
 * @param reference 来源或待修改报价的 ID 和期望版本
 * @param title 报价标题
 * @param quotedAt 报价业务时间
 * @param customer 客户资料
 * @param remark 整单备注
 * @param discount 整单优惠
 * @param items 有序商品明细
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationFormVO(
        QuotationVersionedId reference,
        String title,
        OffsetDateTime quotedAt,
        QuotationCustomerVO customer,
        String remark,
        Discount discount,
        List<Item> items
) {

    /**
     * 两种优惠方式互斥，比例 95 表示 9.5 折。
     */
    public record Discount(
            @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
            BigDecimal discountRate,
            @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
            BigDecimal discountedAmount
    ) {
    }

    /**
     * 沿用原行时提交来源明细 ID，新选时提交 SKU ID 及两个版本。
     */
    public record Item(
            Long sourceItemId,
            Long skuId,
            Long productVersion,
            Long skuVersion,
            @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
            BigDecimal quantity,
            Discount discount,
            String remark
    ) {
    }
}
