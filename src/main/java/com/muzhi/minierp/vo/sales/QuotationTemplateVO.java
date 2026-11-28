package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.QuotationTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 报价模板各操作共用模型。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = QuotationTemplate.class, convertGenerate = false)
public class QuotationTemplateVO extends QuotationTemplate {

    @Serial
    private static final long serialVersionUID = 8487492257288719849L;

    /**
     * 是否由当前登录用户创建
     */
    private Boolean ownedByCurrentUser;

    /**
     * 当前是否可套用
     */
    private Boolean usable;

    /**
     * 受商品异常影响的明细行数
     */
    private Integer issueCount;

    /**
     * 有序模板明细
     */
    private List<QuotationTemplateItemVO> items;
}
