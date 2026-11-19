package com.muzhi.minierp.service.product;

import com.muzhi.minierp.entity.product.ProductImage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品图片，同一商品下 SKU 共用，非空图片列表恰好一张主图由应用事务保证 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductImageService extends IService<ProductImage> {

}
