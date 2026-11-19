package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSpecValue;
import com.muzhi.minierp.mapper.product.ProductSpecValueMapper;
import com.muzhi.minierp.service.product.IProductSpecValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品规格候选值，文本或数值与所属维度类型的一致性由应用校验 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSpecValueServiceImpl extends ServiceImpl<ProductSpecValueMapper, ProductSpecValue> implements IProductSpecValueService {

}
