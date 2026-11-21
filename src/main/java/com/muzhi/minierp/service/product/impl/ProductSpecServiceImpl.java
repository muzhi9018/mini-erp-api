package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.*;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.product.*;
import com.muzhi.minierp.service.product.IProductSpecService;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 商品结构化规格服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductSpecServiceImpl extends ServiceImpl<ProductSpecMapper, ProductSpec> implements IProductSpecService {

    private final ProductSpecValueMapper productSpecValueMapper;

    private final ProductSkuMapper productSkuMapper;

    private final ProductSkuSpecValueMapper productSkuSpecValueMapper;

    private final ProductSnapshotMapper productSnapshotMapper;

    private final ProductMapper productMapper;

    private final IProductUnitService productUnitService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public List<ProductSpecVO> initialize(Long productId, List<ProductSpecVO> specs) {
        List<ProductSpecVO> created = new ArrayList<>();
        for (ProductSpecVO spec : specs) {
            ProductSpecVO saved = this.insertSpec(productId, spec);
            created.add(saved);
        }
        return created;
    }

    @Override
    public List<ProductSpecVO> listByProduct(Long productId) {
        LambdaQueryWrapper<ProductSpec> query = Wrappers.lambdaQuery();
        query.eq(ProductSpec::getProductId, productId);
        query.orderByAsc(ProductSpec::getSortOrder, ProductSpec::getId);
        List<ProductSpec> specs = super.baseMapper.selectList(query);
        List<ProductSpecVO> result = BeanConvertUtils.mapList(specs, ProductSpecVO.class);
        for (ProductSpecVO spec : result) {
            List<ProductSpecValueVO> values = this.listValues(spec.getId());
            spec.setValues(values);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSpecVO create(ProductSpecVO query) {
        Product product = this.lockProduct(query.getProductId(), query.getExpectedVersion());
        this.checkStructureEditable(product.getId());
        ProductSpecVO spec = this.insertSpec(product.getId(), query);
        this.incrementProductVersion(product);
        return spec;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSpecVO updateSpec(ProductSpecVO query) {
        ProductSpec spec = this.requireSpec(query.getId());
        Product product = this.lockProduct(spec.getProductId(), query.getExpectedVersion());
        spec = this.requireSpec(query.getId());
        String name = query.getName().trim();
        this.checkName(spec.getProductId(), name, spec.getId());
        LambdaUpdateWrapper<ProductSpec> update = Wrappers.lambdaUpdate();
        update.eq(ProductSpec::getId, spec.getId());
        update.set(ProductSpec::getName, name);
        if (query.getSortOrder() != null) {
            update.set(ProductSpec::getSortOrder, query.getSortOrder());
        }
        try {
            super.baseMapper.update(new ProductSpec(), update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.spec.name-exists", "同一商品下规格名称不能重复", e);
        }
        this.incrementProductVersion(product);
        ProductSpec updated = super.baseMapper.selectById(spec.getId());
        ProductSpecVO result = BeanConvertUtils.convert(updated, ProductSpecVO.class);
        List<ProductSpecValueVO> values = this.listValues(spec.getId());
        result.setValues(values);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpec(ProductRequests.VersionedId query) {
        ProductSpec spec = this.requireSpec(query.id());
        Product product = this.lockProduct(spec.getProductId(), query.expectedVersion());
        spec = this.requireSpec(query.id());
        this.checkStructureEditable(product.getId());
        LambdaQueryWrapper<ProductSpecValue> values = Wrappers.lambdaQuery();
        values.eq(ProductSpecValue::getSpecId, spec.getId());
        productSpecValueMapper.delete(values);
        super.baseMapper.deleteById(spec);
        this.incrementProductVersion(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSpecValueVO createValue(ProductSpecValueVO query) {
        ProductSpec spec = this.requireSpec(query.getSpecId());
        Product product = this.lockProduct(spec.getProductId(), query.getExpectedVersion());
        spec = this.requireSpec(query.getSpecId());
        ProductSpecValueVO value = this.insertValue(spec, query);
        this.incrementProductVersion(product);
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSpecValueVO updateValue(ProductSpecValueVO query) {
        ProductSpecValue value = this.requireValue(query.getId());
        ProductSpec spec = this.requireSpec(value.getSpecId());
        Product product = this.lockProduct(spec.getProductId(), query.getExpectedVersion());
        value = this.requireValue(query.getId());
        spec = this.requireSpec(value.getSpecId());
        boolean changeValue = query.getTextValue() != null || query.getNumberValue() != null;
        String normalized = changeValue ? this.normalize(spec, query) : value.getNormalizedValue();
        boolean used = this.valueUsed(value.getId());
        boolean valueChanged = !normalized.equals(value.getNormalizedValue());
        Assert.isTrue(used && valueChanged, "product.spec.value-in-use", "已被 SKU 使用的候选值不能改写，只能修改排序");
        this.checkValueDuplicate(spec.getId(), normalized, value.getId());
        LambdaUpdateWrapper<ProductSpecValue> update = Wrappers.lambdaUpdate();
        update.eq(ProductSpecValue::getId, value.getId());
        if (changeValue) {
            String text = query.getTextValue() == null ? null : query.getTextValue().trim();
            update.set(ProductSpecValue::getTextValue, text);
            update.set(ProductSpecValue::getNumberValue, query.getNumberValue());
            update.set(ProductSpecValue::getNormalizedValue, normalized);
        }
        // 仅修改排序时也保留审计和商品版本更新。
        Integer order = query.getSortOrder() == null ? value.getSortOrder() : query.getSortOrder();
        update.set(ProductSpecValue::getSortOrder, order);
        try {
            productSpecValueMapper.update(new ProductSpecValue(), update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.spec.value-duplicate", "同一维度下候选值不能重复", e);
        }
        this.incrementProductVersion(product);
        ProductSpecValue updated = productSpecValueMapper.selectById(value.getId());
        return BeanConvertUtils.convert(updated, ProductSpecValueVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteValue(ProductRequests.VersionedId query) {
        ProductSpecValue value = this.requireValue(query.id());
        ProductSpec spec = this.requireSpec(value.getSpecId());
        Product product = this.lockProduct(spec.getProductId(), query.expectedVersion());
        value = this.requireValue(query.id());
        boolean used = this.valueUsed(value.getId());
        Assert.isTrue(used, "product.spec.value-in-use", "候选值已被 SKU 使用，不能删除");
        productSpecValueMapper.deleteById(value);
        this.incrementProductVersion(product);
    }

    private ProductSpecVO insertSpec(Long productId, ProductSpecVO query) {
        String name = query.getName().trim();
        this.checkName(productId, name, null);
        if (query.getUnitId() != null) {
            productUnitService.requireUnit(query.getUnitId());
        }
        ProductSpec spec = new ProductSpec();
        Long id = IdWorker.getId();
        spec.setId(id);
        spec.setProductId(productId);
        spec.setName(name);
        spec.setValueType(query.getValueType());
        spec.setUnitId(query.getUnitId());
        spec.setSortOrder(query.getSortOrder() == null ? 0 : query.getSortOrder());
        spec.setIsDeleted(false);
        try {
            super.baseMapper.insert(spec);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.spec.name-exists", "同一商品下规格名称不能重复", e);
        }
        List<ProductSpecValueVO> values = new ArrayList<>();
        for (ProductSpecValueVO value : query.getValues()) {
            ProductSpecValueVO saved = this.insertValue(spec, value);
            saved.setClientKey(value.getClientKey());
            values.add(saved);
        }
        ProductSpecVO result = BeanConvertUtils.convert(spec, ProductSpecVO.class);
        result.setClientKey(query.getClientKey());
        result.setValues(values);
        return result;
    }

    private ProductSpecValueVO insertValue(ProductSpec spec, ProductSpecValueVO query) {
        String normalized = this.normalize(spec, query);
        this.checkValueDuplicate(spec.getId(), normalized, null);
        ProductSpecValue value = new ProductSpecValue();
        Long id = IdWorker.getId();
        value.setId(id);
        value.setSpecId(spec.getId());
        value.setTextValue(query.getTextValue() == null ? null : query.getTextValue().trim());
        value.setNumberValue(query.getNumberValue());
        value.setNormalizedValue(normalized);
        value.setSortOrder(query.getSortOrder() == null ? 0 : query.getSortOrder());
        value.setIsDeleted(false);
        try {
            productSpecValueMapper.insert(value);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.spec.value-duplicate", "同一维度下候选值不能重复", e);
        }
        return BeanConvertUtils.convert(value, ProductSpecValueVO.class);
    }

    private String normalize(ProductSpec spec, ProductSpecValue query) {
        ProductEnum.SpecValueType type = ProductEnum.SpecValueType.ofCode(spec.getValueType());
        if (type == ProductEnum.SpecValueType.TEXT) {
            Assert.isTrue(query.getTextValue() == null || query.getNumberValue() != null,
                    "product.spec.value-type-invalid", "文本规格必须填写文本候选值");
            return query.getTextValue().trim();
        }
        Assert.isTrue(query.getNumberValue() == null || query.getTextValue() != null,
                "product.spec.value-type-invalid", "数值规格必须填写数值候选值");
        return query.getNumberValue().stripTrailingZeros().toPlainString();
    }

    private void checkStructureEditable(Long productId) {
        LambdaQueryWrapper<ProductSku> query = Wrappers.lambdaQuery();
        query.eq(ProductSku::getProductId, productId);
        boolean hasSku = productSkuMapper.exists(query);
        Assert.isTrue(hasSku, "product.spec.structure-in-use", "已有 SKU 时不能增删规格维度");
        LambdaQueryWrapper<ProductSnapshot> snapshots = Wrappers.lambdaQuery();
        snapshots.eq(ProductSnapshot::getSourceProductId, productId);
        boolean hasSnapshot = productSnapshotMapper.exists(snapshots);
        Assert.isTrue(hasSnapshot, "product.spec.structure-used-by-business", "已有业务使用的商品不能重建规格，请建立新商品档案");
    }

    private void checkName(Long productId, String name, Long excludedId) {
        LambdaQueryWrapper<ProductSpec> query = Wrappers.lambdaQuery();
        query.eq(ProductSpec::getProductId, productId);
        query.eq(ProductSpec::getName, name);
        query.ne(excludedId != null, ProductSpec::getId, excludedId);
        boolean exists = super.baseMapper.exists(query);
        Assert.isTrue(exists, "product.spec.name-exists", "同一商品下规格名称不能重复");
    }

    private void checkValueDuplicate(Long specId, String normalized, Long excludedId) {
        LambdaQueryWrapper<ProductSpecValue> query = Wrappers.lambdaQuery();
        query.eq(ProductSpecValue::getSpecId, specId);
        query.eq(ProductSpecValue::getNormalizedValue, normalized);
        query.ne(excludedId != null, ProductSpecValue::getId, excludedId);
        boolean exists = productSpecValueMapper.exists(query);
        Assert.isTrue(exists, "product.spec.value-duplicate", "同一维度下候选值不能重复");
    }

    private boolean valueUsed(Long valueId) {
        LambdaQueryWrapper<ProductSkuSpecValue> query = Wrappers.lambdaQuery();
        query.eq(ProductSkuSpecValue::getSpecValueId, valueId);
        return productSkuSpecValueMapper.exists(query);
    }

    private ProductSpec requireSpec(Long id) {
        ProductSpec spec = super.baseMapper.selectById(id);
        Assert.isNull(spec, "product.spec.not-found", "规格维度不存在或已删除");
        return spec;
    }

    private ProductSpecValue requireValue(Long id) {
        ProductSpecValue value = productSpecValueMapper.selectById(id);
        Assert.isNull(value, "product.spec.value-not-found", "规格候选值不存在或已删除");
        return value;
    }

    private List<ProductSpecValueVO> listValues(Long specId) {
        LambdaQueryWrapper<ProductSpecValue> query = Wrappers.lambdaQuery();
        query.eq(ProductSpecValue::getSpecId, specId);
        query.orderByAsc(ProductSpecValue::getSortOrder, ProductSpecValue::getId);
        List<ProductSpecValue> values = productSpecValueMapper.selectList(query);
        return BeanConvertUtils.mapList(values, ProductSpecValueVO.class);
    }

    /**
     * 在当前业务事务内锁定商品并校验客户端确认的商品版本。
     */
    private Product lockProduct(Long productId, Long expectedVersion) {
        Product product = productMapper.selectForUpdate(productId);
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
        int updated = productMapper.update(audit, update);
        Assert.isTrue(updated != 1, "product.version-conflict", "资料已更新，请重新读取后确认");
        product.setVersion(nextVersion);
        return nextVersion;
    }
}
