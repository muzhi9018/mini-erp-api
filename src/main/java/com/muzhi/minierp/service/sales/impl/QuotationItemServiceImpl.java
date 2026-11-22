package com.muzhi.minierp.service.sales.impl;

import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.mapper.sales.QuotationItemMapper;
import com.muzhi.minierp.service.sales.IQuotationItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 正式报价商品明细，使用商品快照和保存时的成交数据 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
public class QuotationItemServiceImpl extends ServiceImpl<QuotationItemMapper, QuotationItem> implements IQuotationItemService {

}
