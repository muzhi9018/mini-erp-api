package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.ProductImage;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.vo.product.ProductImageVO;
import com.muzhi.minierp.vo.product.ProductRequests;
import com.muzhi.minierp.vo.product.ProductVO;

import java.util.List;

/**
 * <p>
 * 商品图片服务，图片关联移除不删除附件
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductImageService extends IService<ProductImage> {

    /**
     * 在调用方事务内替换指定商品或 SKU 的图片。
     */
    void replace(ProductEnum.ImageType imageType, Long ownerId, List<ProductImageVO> images);

    /**
     * 带商品版本校验保存完整列表。
     */
    ProductRequests.ImagesResult saveImages(ProductVO query);

    /**
     * 读取图片并批量生成授权地址。
     */
    List<ProductImageVO> listByProduct(Long productId);

    /**
     * 批量读取各 SKU 自身的图片并生成授权地址。
     */
    List<ProductImageVO> listBySkus(List<Long> skuIds);
}
