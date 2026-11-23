package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.Quotation;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.JSONWriter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 正式报价的摘要，供列表展示和保存结果引用。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
@AutoMapper(target = Quotation.class, convertGenerate = false)
public class QuotationSummaryVO {

    /**
     * 正式报价 ID。
     */
    private Long id;

    /**
     * 正式单号。
     */
    private String quoteNumber;

    /**
     * 报价标题。
     */
    private String title;

    /**
     * 报价业务时间。
     */
    private OffsetDateTime quotedAt;

    /**
     * 报价币种，见 {@link com.muzhi.minierp.enums.CurrencyEnum}。
     */
    private String currency;

    /**
     * 客户名称。
     */
    private String customerName;

    /**
     * 最终报价金额。
     */
    @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
    private BigDecimal totalAmount;

    /**
     * 是否已生成订单。
     */
    private Boolean orderGenerated;

    /**
     * 报价版本，供列表直接发起删除。
     */
    private Long version;
}
