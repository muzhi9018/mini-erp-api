package com.muzhi.minierp.service.sales;

import com.muzhi.minierp.entity.sales.QuotationTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.enums.QuotationEnum.Scope;
import com.muzhi.minierp.vo.sales.QuotationTemplateVO;
import com.muzhi.minierp.vo.sales.QuotationVO;

/**
 * <p>
 * 个人或公共报价模板，保存 SKU 组合及整单折扣 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public interface IQuotationTemplateService extends IService<QuotationTemplate> {

    /**
     * 分页读取可见模板并批量判断当前可用性。
     */
    IPage<QuotationTemplateVO> list(Scope scope, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 读取模板、已保存的明细及实时问题。
     */
    QuotationTemplateVO detail(Scope scope, Long id);

    /**
     * 创建指定范围的模板。
     */
    QuotationTemplateVO create(Scope scope, QuotationTemplateVO form);

    /**
     * 校验期望版本后更新模板和完整的有序明细。
     */
    QuotationTemplateVO update(Scope scope, QuotationTemplateVO form);

    /**
     * 按范围和期望版本逻辑删除当前用户创建的模板及其有效明细。
     */
    void delete(Scope scope, QuotationTemplateVO reference);

    /**
     * 根据实时选品资料生成报价编辑页的套用值。
     */
    QuotationVO apply(Scope scope, Long id);
}
