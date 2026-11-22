package com.muzhi.minierp.service.sales.impl;

import com.muzhi.minierp.entity.sales.QuotationTemplateItem;
import com.muzhi.minierp.mapper.sales.QuotationTemplateItemMapper;
import com.muzhi.minierp.service.sales.IQuotationTemplateItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 报价模板商品明细，同一 SKU 可在多个位置使用不同折扣 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
public class QuotationTemplateItemServiceImpl extends ServiceImpl<QuotationTemplateItemMapper, QuotationTemplateItem> implements IQuotationTemplateItemService {

}
