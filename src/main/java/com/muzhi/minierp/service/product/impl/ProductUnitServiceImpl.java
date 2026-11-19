package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductUnit;
import com.muzhi.minierp.mapper.product.ProductUnitMapper;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品计量单位，供 SKU 数量和数值规格使用，不提供单位换算 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductUnitServiceImpl extends ServiceImpl<ProductUnitMapper, ProductUnit> implements IProductUnitService {

}
