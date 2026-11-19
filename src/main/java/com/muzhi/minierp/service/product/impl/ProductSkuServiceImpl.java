package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSku;
import com.muzhi.minierp.mapper.product.ProductSkuMapper;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 内部商品 SKU，库存为人工维护的当前数量，不自动增减或预占 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements IProductSkuService {

}
