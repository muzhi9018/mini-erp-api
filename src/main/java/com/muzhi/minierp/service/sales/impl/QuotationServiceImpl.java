package com.muzhi.minierp.service.sales.impl;

import com.muzhi.minierp.entity.sales.Quotation;
import com.muzhi.minierp.mapper.sales.QuotationMapper;
import com.muzhi.minierp.service.sales.IQuotationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 正式报价单，保存客户、开单方及计价结果作为历史记录 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
public class QuotationServiceImpl extends ServiceImpl<QuotationMapper, Quotation> implements IQuotationService {

}
