package com.muzhi.minierp.service.sales;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.model.sales.QuotationSkuSelection;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 正式报价商品明细，使用商品快照和保存时的成交数据 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public interface IQuotationItemService extends IService<QuotationItem> {

    /**
     * 在当前报价或模板事务中批量检查 SKU。写入时先按 ID 锁商品，再按商品 ID 和 SKU ID 锁定 SKU。
     *
     * @param skuIds 待读取的 SKU ID
     * @param forWrite 是否为保存操作加锁
     * @return 按 SKU ID 索引的当前选品资料与不可用原因
     */
    Map<Long, QuotationSkuSelection> loadSkuSelections(List<Long> skuIds, boolean forWrite);
}
