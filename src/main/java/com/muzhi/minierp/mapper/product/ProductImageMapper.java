package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductImage;

/**
 * <p>
 * 商品图片，同一商品下 SKU 共用，非空图片列表恰好一张主图由应用事务保证 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductImageMapper extends BaseMapper<ProductImage> {

}
