package com.muzhi.minierp.vo.sales;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 报价列表筛选条件，日期按台北业务时间解释。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
public class QuotationQueryVO {

    /**
     * 关键字同时匹配单号、标题和客户名称。
     */
    private String keyword;

    /**
     * 正式单号包含指定文本。
     */
    private String quoteNumber;

    /**
     * 报价标题包含指定文本。
     */
    private String title;

    /**
     * 客户名称包含指定文本。
     */
    private String customerName;

    /**
     * 报价起始日期，包含边界。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate quotedFrom;

    /**
     * 报价结束日期，包含边界。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate quotedTo;

    /**
     * 是否已生成订单。
     */
    private Boolean orderGenerated;
}
