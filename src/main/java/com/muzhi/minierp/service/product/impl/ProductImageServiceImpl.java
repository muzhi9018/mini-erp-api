package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.Product;
import com.muzhi.minierp.entity.product.ProductImage;
import com.muzhi.minierp.entity.system.Attachment;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.mapper.product.ProductImageMapper;
import com.muzhi.minierp.mapper.product.ProductMapper;
import com.muzhi.minierp.service.product.IProductImageService;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductImageVO;
import com.muzhi.minierp.vo.product.ProductRequests;
import com.muzhi.minierp.vo.product.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 商品完整图片列表服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements IProductImageService {

    private final ProductMapper productMapper;

    private final IAttachmentService attachmentService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void replace(ProductEnum.ImageType imageType, Long ownerId, List<ProductImageVO> images) {
        List<Long> ids = images.stream().map(ProductImageVO::getAttachmentId).toList();
        if (!ids.isEmpty()) {
            List<Attachment> attachments = attachmentService.listByIds(ids);
            Assert.isTrue(attachments.size() != ids.size(), "product.image.not-found", "图片附件不存在或已删除");
            Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "image/bmp");
            for (Attachment attachment : attachments) {
                String contentType = attachment.getContentType();
                boolean invalid = contentType == null || !allowedTypes.contains(contentType.toLowerCase(java.util.Locale.ROOT));
                Assert.isTrue(invalid, "product.image.type-invalid", "附件必须为支持的图片类型");
            }
        }
        List<ProductImage> current = this.findImages(imageType, ownerId);
        Map<Long, ProductImage> byAttachment = new HashMap<>();
        for (ProductImage image : current) {
            byAttachment.put(image.getAttachmentId(), image);
        }
        // 先解除旧主图，避免切换时违反单主图唯一索引。
        LambdaUpdateWrapper<ProductImage> clearCover = Wrappers.lambdaUpdate();
        clearCover.eq(ProductImage::getImageType, imageType.getCode());
        clearCover.eq(ProductImage::getOwnerId, ownerId);
        clearCover.eq(ProductImage::getIsCover, true);
        clearCover.set(ProductImage::getIsCover, false);
        super.baseMapper.update(new ProductImage(), clearCover);

        for (ProductImage image : current) {
            if (!ids.contains(image.getAttachmentId())) {
                super.baseMapper.deleteById(image);
            }
        }
        boolean explicitCover = images.stream().anyMatch(image -> Boolean.TRUE.equals(image.getIsCover()));
        int order = 0;
        for (ProductImageVO input : images) {
            ProductImage existing = byAttachment.get(input.getAttachmentId());
            boolean cover = explicitCover ? Boolean.TRUE.equals(input.getIsCover()) : order == 0;
            if (existing == null) {
                ProductImage image = new ProductImage();
                Long id = IdWorker.getId();
                image.setId(id);
                image.setImageType(imageType.getCode());
                image.setOwnerId(ownerId);
                image.setAttachmentId(input.getAttachmentId());
                image.setIsCover(cover);
                image.setSortOrder(order);
                image.setIsDeleted(false);
                super.baseMapper.insert(image);
            } else {
                LambdaUpdateWrapper<ProductImage> update = Wrappers.lambdaUpdate();
                update.eq(ProductImage::getId, existing.getId());
                update.set(ProductImage::getIsCover, cover);
                update.set(ProductImage::getSortOrder, order);
                super.baseMapper.update(new ProductImage(), update);
            }
            order++;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.ImagesResult saveImages(ProductVO query) {
        Product product = productMapper.selectForUpdate(query.getId());
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        boolean versionConflict = !Objects.equals(product.getVersion(), query.getExpectedVersion());
        Assert.isTrue(versionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        this.replace(ProductEnum.ImageType.PRODUCT, product.getId(), query.getImages());

        LambdaUpdateWrapper<Product> versionUpdate = Wrappers.lambdaUpdate();
        versionUpdate.eq(Product::getId, product.getId());
        versionUpdate.eq(Product::getVersion, product.getVersion());
        Long version = Math.incrementExact(product.getVersion());
        versionUpdate.set(Product::getVersion, version);
        Product audit = new Product();
        int updated = productMapper.update(audit, versionUpdate);
        Assert.isTrue(updated != 1, "product.version-conflict", "资料已更新，请重新读取后确认");
        List<ProductImageVO> images = this.listByProduct(product.getId());
        return new ProductRequests.ImagesResult(product.getId(), version, images);
    }

    @Override
    public List<ProductImageVO> listByProduct(Long productId) {
        List<ProductImage> images = this.findImages(ProductEnum.ImageType.PRODUCT, productId);
        return this.authorizeImages(images);
    }

    @Override
    public List<ProductImageVO> listBySkus(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<ProductImage> query = Wrappers.lambdaQuery();
        query.eq(ProductImage::getImageType, ProductEnum.ImageType.SKU.getCode());
        query.in(ProductImage::getOwnerId, skuIds);
        query.orderByAsc(ProductImage::getSortOrder, ProductImage::getId);
        List<ProductImage> images = super.baseMapper.selectList(query);
        return this.authorizeImages(images);
    }

    /**
     * 转换图片并批量生成附件访问地址。
     */
    private List<ProductImageVO> authorizeImages(List<ProductImage> images) {
        if (images.isEmpty()) {
            return List.of();
        }

        List<ProductImageVO> result = BeanConvertUtils.mapList(images, ProductImageVO.class);
        List<Long> ids = images.stream().map(ProductImage::getAttachmentId).toList();
        Map<Long, String> urls = attachmentService.getAuthorizedUrls(ids);
        for (ProductImageVO image : result) {
            image.setUrl(urls.get(image.getAttachmentId()));
        }
        return result;
    }

    /**
     * 查询指定归属的当前有效图片。
     */
    private List<ProductImage> findImages(ProductEnum.ImageType imageType, Long ownerId) {
        LambdaQueryWrapper<ProductImage> query = Wrappers.lambdaQuery();
        query.eq(ProductImage::getImageType, imageType.getCode());
        query.eq(ProductImage::getOwnerId, ownerId);
        query.orderByAsc(ProductImage::getSortOrder, ProductImage::getId);
        return super.baseMapper.selectList(query);
    }
}
