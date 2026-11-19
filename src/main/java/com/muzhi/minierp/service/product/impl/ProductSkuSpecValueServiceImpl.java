package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSkuSpecValue;
import com.muzhi.minierp.mapper.product.ProductSkuSpecValueMapper;
import com.muzhi.minierp.service.product.IProductSkuSpecValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * SKU 规格选择关系，商品归属和维度选择完整性由应用事务保证 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSkuSpecValueServiceImpl extends ServiceImpl<ProductSkuSpecValueMapper, ProductSkuSpecValue> implements IProductSkuSpecValueService {

}
