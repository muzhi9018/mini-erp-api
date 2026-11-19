package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductImage;
import com.muzhi.minierp.mapper.product.ProductImageMapper;
import com.muzhi.minierp.service.product.IProductImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品图片，同一商品下 SKU 共用，非空图片列表恰好一张主图由应用事务保证 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements IProductImageService {

}
