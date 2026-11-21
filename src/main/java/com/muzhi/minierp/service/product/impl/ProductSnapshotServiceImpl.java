package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.*;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.mapper.product.*;
import com.muzhi.minierp.service.product.IProductSnapshotService;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * <p>
 * 商品快照服务实现类，主表及明细只插入，不随主档变化
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductSnapshotServiceImpl extends ServiceImpl<ProductSnapshotMapper, ProductSnapshot> implements IProductSnapshotService {

    private final ProductSnapshotSpecMapper productSnapshotSpecMapper;

    private final ProductSnapshotImageMapper productSnapshotImageMapper;

    private final ProductCategoryMapper productCategoryMapper;

    private final ProductSpecMapper productSpecMapper;

    private final ProductSkuSpecValueMapper productSkuSpecValueMapper;

    private final ProductImageMapper productImageMapper;

    private final IAttachmentService attachmentService;

    private final ProductMapper productMapper;

    private final ProductSkuMapper productSkuMapper;

    private final IProductUnitService productUnitService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ProductSnapshotVO getOrCreate(Long skuId, Long expectedProductVersion, Long expectedSkuVersion) {
        ProductSku source = productSkuMapper.selectById(skuId);
        Assert.isNull(source, "product.sku.not-found", "SKU 不存在或已删除");
        Product product = productMapper.selectForUpdate(source.getProductId());
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        ProductSku sku = productSkuMapper.selectForUpdate(skuId);
        Assert.isNull(sku, "product.sku.not-found", "SKU 不存在或已删除");
        boolean skuVersionConflict = !Objects.equals(sku.getVersion(), expectedSkuVersion);
        Assert.isTrue(skuVersionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        boolean productVersionConflict = !Objects.equals(product.getVersion(), expectedProductVersion);
        Assert.isTrue(productVersionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        boolean enabled = ProductEnum.Status.ENABLED.getCode().equals(product.getStatus())
                && ProductEnum.Status.ENABLED.getCode().equals(sku.getStatus());
        Assert.isTrue(!enabled, "product.snapshot.source-disabled", "商品或 SKU 已停用，请重新选品");
        LambdaQueryWrapper<ProductSnapshot> existingQuery = Wrappers.lambdaQuery();
        existingQuery.eq(ProductSnapshot::getSourceSkuId, skuId);
        existingQuery.eq(ProductSnapshot::getVersion, product.getVersion());
        ProductSnapshot existing = super.baseMapper.selectOne(existingQuery);
        if (existing != null) {
            return this.readForDocument(existing.getId());
        }

        ProductUnit unit = productUnitService.requireUnit(sku.getUnitId());
        ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
        Assert.isNull(category, "product.category.not-found", "商品类目不存在或已删除");
        List<ProductSkuSpecValueVO> selections = productSkuSpecValueMapper.selectDetails(List.of(skuId));
        ProductSnapshot snapshot = new ProductSnapshot();
        Long id = IdWorker.getId();
        snapshot.setId(id);
        snapshot.setSourceProductId(product.getId());
        snapshot.setSourceSkuId(skuId);
        snapshot.setVersion(product.getVersion());
        snapshot.setSourceSkuVersion(sku.getVersion());
        snapshot.setProductCode(product.getCode());
        snapshot.setSkuCode(sku.getCode());
        snapshot.setProductName(product.getName());
        snapshot.setBrand(product.getBrand());
        snapshot.setModel(sku.getModel());
        snapshot.setDescription(product.getDescription());
        snapshot.setCategoryCode(category.getCode());
        snapshot.setCategoryName(category.getName());
        String path = this.categoryPath(category);
        snapshot.setCategoryPath(path);
        snapshot.setUnitCode(unit.getCode());
        snapshot.setUnitName(unit.getName());
        snapshot.setQuantityScale(sku.getQuantityScale());
        String text = this.specificationText(selections);
        snapshot.setSpecificationText(text);
        snapshot.setIsDeleted(false);
        super.baseMapper.insert(snapshot);
        int order = 0;
        for (ProductSkuSpecValueVO selection : selections) {
            ProductSnapshotSpec spec = new ProductSnapshotSpec();
            Long specId = IdWorker.getId();
            spec.setId(specId);
            spec.setSnapshotId(snapshot.getId());
            spec.setSourceSpecId(selection.getSpecId());
            spec.setSourceSpecValueId(selection.getSpecValueId());
            spec.setSpecName(selection.getSpecName());
            spec.setValueType(selection.getValueType());
            spec.setTextValue(selection.getTextValue());
            spec.setNumberValue(selection.getNumberValue());
            ProductSpec sourceSpec = productSpecMapper.selectById(selection.getSpecId());
            if (sourceSpec.getUnitId() != null) {
                ProductUnit specUnit = productUnitService.requireUnit(sourceSpec.getUnitId());
                spec.setUnitCode(specUnit.getCode());
                spec.setUnitName(specUnit.getName());
            }
            spec.setSortOrder(order++);
            spec.setIsDeleted(false);
            productSnapshotSpecMapper.insert(spec);
        }
        LambdaQueryWrapper<ProductImage> imagesQuery = Wrappers.lambdaQuery();
        imagesQuery.nested(ownership -> {
            ownership.eq(ProductImage::getImageType, ProductEnum.ImageType.PRODUCT.getCode());
            ownership.eq(ProductImage::getOwnerId, product.getId());
            ownership.or(skuImages -> {
                skuImages.eq(ProductImage::getImageType, ProductEnum.ImageType.SKU.getCode());
                skuImages.eq(ProductImage::getOwnerId, skuId);
            });
        });
        imagesQuery.orderByAsc(ProductImage::getImageType, ProductImage::getSortOrder, ProductImage::getId);
        List<ProductImage> images = productImageMapper.selectList(imagesQuery);
        for (ProductImage image : images) {
            ProductSnapshotImage saved = new ProductSnapshotImage();
            Long imageId = IdWorker.getId();
            saved.setId(imageId);
            saved.setSnapshotId(snapshot.getId());
            saved.setImageSource(image.getImageType());
            saved.setSourceImageId(image.getId());
            saved.setAttachmentId(image.getAttachmentId());
            saved.setIsCover(image.getIsCover());
            saved.setSortOrder(image.getSortOrder());
            saved.setIsDeleted(false);
            productSnapshotImageMapper.insert(saved);
        }
        return this.readForDocument(snapshot.getId());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProductSnapshotVO readForDocument(Long snapshotId) {
        ProductSnapshot snapshot = super.baseMapper.selectById(snapshotId);
        Assert.isNull(snapshot, "product.snapshot.not-found", "商品快照不存在");
        ProductSnapshotVO result = BeanConvertUtils.convert(snapshot, ProductSnapshotVO.class);
        LambdaQueryWrapper<ProductSnapshotSpec> specsQuery = Wrappers.lambdaQuery();
        specsQuery.eq(ProductSnapshotSpec::getSnapshotId, snapshotId);
        specsQuery.orderByAsc(ProductSnapshotSpec::getSortOrder, ProductSnapshotSpec::getId);
        List<ProductSnapshotSpec> specs = productSnapshotSpecMapper.selectList(specsQuery);
        result.setSpecs(specs);
        LambdaQueryWrapper<ProductSnapshotImage> imagesQuery = Wrappers.lambdaQuery();
        imagesQuery.eq(ProductSnapshotImage::getSnapshotId, snapshotId);
        imagesQuery.orderByAsc(ProductSnapshotImage::getImageSource, ProductSnapshotImage::getSortOrder, ProductSnapshotImage::getId);
        List<ProductSnapshotImage> images = productSnapshotImageMapper.selectList(imagesQuery);
        List<ProductSnapshotImageVO> views = BeanConvertUtils.mapList(images, ProductSnapshotImageVO.class);
        List<Long> attachmentIds = images.stream().map(ProductSnapshotImage::getAttachmentId).toList();
        Map<Long, String> urls = attachmentService.getAuthorizedUrls(attachmentIds);
        for (ProductSnapshotImageVO image : views) {
            image.setUrl(urls.get(image.getAttachmentId()));
        }
        result.setImages(views);
        return result;
    }

    private String categoryPath(ProductCategory category) {
        List<String> names = new ArrayList<>();
        ProductCategory current = category;
        int maximumDepth = 3;
        while (current != null) {
            names.add(current.getName());
            Assert.isTrue(names.size() > maximumDepth, "product.category.path-invalid", "商品类目层级数据异常");
            if (current.getParentId().equals(0L)) {
                break;
            }
            current = productCategoryMapper.selectById(current.getParentId());
            Assert.isNull(current, "product.category.path-invalid", "商品类目父级不存在");
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private String specificationText(List<ProductSkuSpecValueVO> selections) {
        StringJoiner text = new StringJoiner(" / ");
        for (ProductSkuSpecValueVO selection : selections) {
            String value = selection.getTextValue();
            if (selection.getNumberValue() != null) {
                value = selection.getNumberValue().stripTrailingZeros().toPlainString();
            }
            String unit = selection.getUnitName() == null ? "" : selection.getUnitName();
            text.add(selection.getSpecName() + ":" + value + unit);
        }
        return text.toString();
    }
}
