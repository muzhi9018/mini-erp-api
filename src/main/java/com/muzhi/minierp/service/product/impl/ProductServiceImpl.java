package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.Product;
import com.muzhi.minierp.mapper.product.ProductMapper;
import com.muzhi.minierp.service.product.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 内部商品档案，与官网商品独立维护 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

}
