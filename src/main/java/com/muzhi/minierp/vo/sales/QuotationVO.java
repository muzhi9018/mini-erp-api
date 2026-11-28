package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.Quotation;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

/**
 * 报价各操作共用模型，服务端按操作白名单处理可写字段。
 *
 * @author Mr.Muzhi
 * @since 2026-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Quotation.class, convertGenerate = false)
public class QuotationVO extends Quotation {

    @Serial
    private static final long serialVersionUID = 593695307342478010L;
    
    /**
     * 开单方现行 Logo 地址
     */
    private String issuerLogoUrl;

    /**
     * 有序商品明细
     */
    private List<QuotationItemVO> items;

    /**
     * 复制草稿的原报价 ID
     */
    private Long sourceId;

    /**
     * 复制草稿的原报价版本
     */
    private Long sourceVersion;

    /**
     * 复制时是否清空客户资料，复制接口必填
     */
    private Boolean clearCustomer;

    /**
     * 列表关键字，同时匹配单号、标题和客户名称
     */
    private String keyword;

    /**
     * 列表报价起始日期，包含边界
     */
    private LocalDate quotedFrom;

    /**
     * 列表报价结束日期，包含边界
     */
    private LocalDate quotedTo;
}
