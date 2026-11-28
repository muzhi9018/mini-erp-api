package com.muzhi.minierp.service.sales;

import com.muzhi.minierp.entity.sales.Quotation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.vo.sales.*;

/**
 * <p>
 * 正式报价单，保存客户、开单方及计价结果作为历史记录 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public interface IQuotationService extends IService<Quotation> {

    /**
     * 预览新增或复制草稿，不写入报价与快照。
     */
    QuotationVO preview(QuotationVO form);

    /**
     * 原子保存新报价、商品快照和明细，并取得正式单号。
     */
    QuotationVO create(QuotationVO form);

    /**
     * 按登录用户分页查询其报价。
     */
    IPage<QuotationVO> list(QuotationVO query, Integer pageNum, Integer pageSize);

    /**
     * 读取报价主表及附带冻结商品资料的明细列表。
     */
    QuotationVO detail(Long id);

    /**
     * 预览修改后的报价，沿用原币种、开单方和编号。
     */
    QuotationVO previewUpdate(QuotationVO form);

    /**
     * 锁定报价后，按期望版本更新可写字段和明细。
     */
    QuotationVO update(QuotationVO form);

    /**
     * 逻辑删除未转单的报价，保留已发单号及历史明细。
     */
    void delete(QuotationVO reference);

    /**
     * 生成带原报价引用的平铺复制草稿，按请求清除原客户资料。
     */
    QuotationVO copy(QuotationVO request);
}
