package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.*;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.product.*;
import com.muzhi.minierp.service.product.*;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * <p>
 * 内部商品档案服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private final ProductCategoryMapper productCategoryMapper;

    private final ProductSkuMapper productSkuMapper;

    private final ProductSpecMapper productSpecMapper;

    private final ProductSpecValueMapper productSpecValueMapper;

    private final ProductSkuSpecValueMapper productSkuSpecValueMapper;

    private final ProductImageMapper productImageMapper;

    private final IProductSpecService productSpecService;

    private final IProductSkuService productSkuService;

    private final IProductImageService productImageService;

    private final IAttachmentService attachmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO create(ProductVO query) {
        this.checkCategory(query.getCategoryId());
        String code = query.getCode().trim();
        boolean codeExists = super.baseMapper.existsByCode(code);
        Assert.isTrue(codeExists, "product.code-exists", "商品编码已存在，删除后也不能复用");
        Product product = new Product();
        Long id = IdWorker.getId();
        product.setId(id);
        product.setCode(code);
        product.setCategoryId(query.getCategoryId());
        product.setName(query.getName().trim());
        product.setBrand(StringUtils.trimToNull(query.getBrand()));
        product.setDescription(StringUtils.trimToNull(query.getDescription()));
        product.setStatus(query.getStatus());
        product.setSortOrder(query.getSortOrder() == null ? 0 : query.getSortOrder());
        product.setVersion(0L);
        product.setIsDeleted(false);
        try {
            super.baseMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.code-exists", "商品编码已存在，删除后也不能复用", e);
        }
        if (!query.getImages().isEmpty()) {
            productImageService.replace(ProductEnum.ImageType.PRODUCT, product.getId(), query.getImages());
        }
        List<ProductSpecVO> specs = productSpecService.initialize(product.getId(), query.getSpecs());
        productSkuService.initialize(product.getId(), query.getSkus(), specs);
        return this.detail(product.getId());
    }

    @Override
    public IPage<ProductVO> list(ProductQueryVO query, Integer pageNum, Integer pageSize) {
        List<Long> categoryIds = null;
        if (query.getCategoryId() != null) {
            categoryIds = productCategoryMapper.selectSubtreeIds(query.getCategoryId());
            if (categoryIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
        }
        query.setKeyword(StringUtils.trimToNull(query.getKeyword()));
        Page<ProductVO> page = new Page<>(pageNum, pageSize);
        IPage<ProductVO> result = super.baseMapper.selectProductPage(page, query, categoryIds);
        List<Long> imageIds = result.getRecords().stream().map(ProductVO::getCoverImageAttachmentId).toList();
        Map<Long, String> urls = attachmentService.getAuthorizedUrls(imageIds);
        for (ProductVO product : result.getRecords()) {
            product.setCoverImageUrl(urls.get(product.getCoverImageAttachmentId()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProductVO detail(Long productId) {
        Product product = super.baseMapper.selectById(productId);
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        ProductVO result = BeanConvertUtils.convert(product, ProductVO.class);
        ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
        result.setCategoryName(category == null ? null : category.getName());
        List<ProductImageVO> images = productImageService.listByProduct(productId);
        result.setImages(images);
        for (ProductImageVO image : images) {
            if (Boolean.TRUE.equals(image.getIsCover())) {
                result.setCoverImageAttachmentId(image.getAttachmentId());
                result.setCoverImageUrl(image.getUrl());
            }
        }
        List<ProductSpecVO> specs = productSpecService.listByProduct(productId);
        result.setSpecs(specs);
        List<ProductSkuVO> skus = productSkuService.listByProduct(productId);
        result.setSkus(skus);
        result.setSkuCount((long) skus.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(ProductVO query) {
        Product product = this.lockProduct(query.getId(), query.getExpectedVersion());
        this.checkCategory(query.getCategoryId());
        LambdaUpdateWrapper<Product> update = Wrappers.lambdaUpdate();
        update.eq(Product::getId, product.getId());
        update.eq(Product::getVersion, product.getVersion());
        update.set(Product::getCategoryId, query.getCategoryId());
        update.set(Product::getName, query.getName().trim());
        update.set(Product::getBrand, StringUtils.trimToNull(query.getBrand()));
        update.set(Product::getDescription, StringUtils.trimToNull(query.getDescription()));
        if (query.getSortOrder() != null) {
            update.set(Product::getSortOrder, query.getSortOrder());
        }
        super.baseMapper.update(new Product(), update);
        this.incrementProductVersion(product);
        return super.baseMapper.selectById(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product changeStatus(ProductVO query) {
        Product product = this.lockProduct(query.getId(), query.getExpectedVersion());
        LambdaUpdateWrapper<Product> update = Wrappers.lambdaUpdate();
        update.eq(Product::getId, product.getId());
        update.eq(Product::getVersion, product.getVersion());
        update.set(Product::getStatus, query.getStatus());
        super.baseMapper.update(new Product(), update);
        this.incrementProductVersion(product);
        return super.baseMapper.selectById(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long delete(ProductRequests.VersionedId query) {
        Product product = this.lockProduct(query.id(), query.expectedVersion());
        LambdaQueryWrapper<ProductSku> skuQuery = Wrappers.lambdaQuery();
        skuQuery.eq(ProductSku::getProductId, product.getId());
        skuQuery.orderByAsc(ProductSku::getId);
        List<ProductSku> skus = productSkuMapper.selectList(skuQuery);
        for (ProductSku sku : skus) {
            Assert.isTrue(sku.getStockQuantity().signum() != 0, "product.has-stock", "存在库存不为0的 SKU，不能删除商品");
        }
        for (ProductSku sku : skus) {
            LambdaQueryWrapper<ProductSkuSpecValue> selections = Wrappers.lambdaQuery();
            selections.eq(ProductSkuSpecValue::getSkuId, sku.getId());
            productSkuSpecValueMapper.delete(selections);
            LambdaUpdateWrapper<ProductSku> skuUpdate = Wrappers.lambdaUpdate();
            skuUpdate.eq(ProductSku::getId, sku.getId());
            skuUpdate.eq(ProductSku::getVersion, sku.getVersion());
            skuUpdate.set(ProductSku::getIsDeleted, true);
            Long nextSkuVersion = Math.incrementExact(sku.getVersion());
            skuUpdate.set(ProductSku::getVersion, nextSkuVersion);
            productSkuMapper.update(new ProductSku(), skuUpdate);
        }
        LambdaQueryWrapper<ProductSpec> specQuery = Wrappers.lambdaQuery();
        specQuery.eq(ProductSpec::getProductId, product.getId());
        List<ProductSpec> specs = productSpecMapper.selectList(specQuery);
        for (ProductSpec spec : specs) {
            LambdaQueryWrapper<ProductSpecValue> values = Wrappers.lambdaQuery();
            values.eq(ProductSpecValue::getSpecId, spec.getId());
            productSpecValueMapper.delete(values);
            productSpecMapper.deleteById(spec);
        }
        LambdaQueryWrapper<ProductImage> productImages = Wrappers.lambdaQuery();
        productImages.eq(ProductImage::getImageType, ProductEnum.ImageType.PRODUCT.getCode());
        productImages.eq(ProductImage::getOwnerId, product.getId());
        productImageMapper.delete(productImages);

        if (!skus.isEmpty()) {
            List<Long> skuIds = skus.stream().map(ProductSku::getId).toList();
            LambdaQueryWrapper<ProductImage> skuImages = Wrappers.lambdaQuery();
            skuImages.eq(ProductImage::getImageType, ProductEnum.ImageType.SKU.getCode());
            skuImages.in(ProductImage::getOwnerId, skuIds);
            productImageMapper.delete(skuImages);
        }

        Long version = this.incrementProductVersion(product);
        super.baseMapper.deleteById(product);
        return version;
    }

    private void checkCategory(Long categoryId) {
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        Assert.isNull(category, "product.category.not-found", "商品类目不存在或已删除");
    }

    /**
     * 在当前业务事务内锁定商品并校验客户端确认的商品版本。
     */
    private Product lockProduct(Long productId, Long expectedVersion) {
        Product product = super.baseMapper.selectForUpdate(productId);
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        boolean versionConflict = !Objects.equals(product.getVersion(), expectedVersion);
        Assert.isTrue(versionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        return product;
    }

    /**
     * 带期望版本原子递增所属商品版本，与当前业务修改一起提交。
     */
    private Long incrementProductVersion(Product product) {
        LambdaUpdateWrapper<Product> update = Wrappers.lambdaUpdate();
        update.eq(Product::getId, product.getId());
        update.eq(Product::getVersion, product.getVersion());
        Long nextVersion = Math.incrementExact(product.getVersion());
        update.set(Product::getVersion, nextVersion);
        Product audit = new Product();
        int updated = super.baseMapper.update(audit, update);
        Assert.isTrue(updated != 1, "product.version-conflict", "资料已更新，请重新读取后确认");
        product.setVersion(nextVersion);
        return nextVersion;
    }
}
