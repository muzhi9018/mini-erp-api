package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.*;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.product.*;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 商品计量单位服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductUnitServiceImpl extends ServiceImpl<ProductUnitMapper, ProductUnit> implements IProductUnitService {

    private final ProductMapper productMapper;

    @Override
    public List<ProductUnit> listUnits() {
        LambdaQueryWrapper<ProductUnit> query = Wrappers.lambdaQuery();
        query.orderByAsc(ProductUnit::getSortOrder, ProductUnit::getId);
        return super.baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductUnit create(ProductUnit query) {
        String code = query.getCode().trim();
        boolean exists = super.baseMapper.existsByCode(code);
        Assert.isTrue(exists, "product.unit.code-exists", "单位编码已存在，删除后也不能复用");
        String name = query.getName().trim();
        this.checkName(name, null);
        ProductUnit unit = new ProductUnit();
        Long id = IdWorker.getId();
        unit.setId(id);
        unit.setCode(code);
        unit.setName(name);
        unit.setSortOrder(query.getSortOrder() == null ? 0 : query.getSortOrder());
        unit.setIsDeleted(false);
        try {
            super.baseMapper.insert(unit);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.unit.duplicate", "单位编码或名称已存在", e);
        }
        return unit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductUnit updateUnit(ProductUnit query) {
        List<Product> products = this.lockAffectedProducts(query.getId());
        ProductUnit unit = this.lockUnit(query.getId(), products);
        String name = query.getName().trim();
        this.checkName(name, unit.getId());
        boolean nameChanged = !unit.getName().equals(name);
        LambdaUpdateWrapper<ProductUnit> update = Wrappers.lambdaUpdate();
        update.eq(ProductUnit::getId, unit.getId());
        update.set(ProductUnit::getName, name);
        if (query.getSortOrder() != null) {
            update.set(ProductUnit::getSortOrder, query.getSortOrder());
        }
        try {
            super.baseMapper.update(new ProductUnit(), update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.unit.name-exists", "单位名称已存在", e);
        }
        if (nameChanged) {
            for (Product product : products) {
                LambdaUpdateWrapper<Product> versionUpdate = Wrappers.lambdaUpdate();
                versionUpdate.eq(Product::getId, product.getId());
                versionUpdate.eq(Product::getVersion, product.getVersion());
                Long nextVersion = Math.incrementExact(product.getVersion());
                versionUpdate.set(Product::getVersion, nextVersion);
                Product audit = new Product();
                int updated = productMapper.update(audit, versionUpdate);
                Assert.isTrue(updated != 1, "product.version-conflict", "资料已更新，请重新读取后确认");
                product.setVersion(nextVersion);
            }
        }
        return super.baseMapper.selectById(unit.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        List<Product> products = this.lockAffectedProducts(id);
        ProductUnit unit = this.lockUnit(id, products);
        Assert.isTrue(!products.isEmpty(), "product.unit.in-use", "单位已被规格或 SKU 使用，不能删除");
        super.baseMapper.deleteById(unit);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ProductUnit requireUnit(Long id) {
        ProductUnit unit = super.baseMapper.selectForShare(id);
        Assert.isNull(unit, "product.unit.not-found", "计量单位不存在或已删除");
        return unit;
    }

    private List<Product> lockAffectedProducts(Long unitId) {
        List<Long> ids = productMapper.selectIdsByUnit(unitId);
        List<Product> products = new ArrayList<>();
        for (Long id : ids) {
            Product product = productMapper.selectForUpdate(id);
            Assert.isNull(product, "product.not-found", "商品不存在或已删除");
            products.add(product);
        }
        return products;
    }

    private ProductUnit lockUnit(Long id, List<Product> products) {
        ProductUnit unit = super.baseMapper.selectForUpdate(id);
        Assert.isNull(unit, "product.unit.not-found", "计量单位不存在或已删除");
        List<Long> lockedIds = products.stream().map(Product::getId).toList();
        List<Long> currentIds = productMapper.selectIdsByUnit(id);
        // 引用在取得单位锁前发生变化时整体回滚，避免漏掉共享资料版本联动。
        Assert.isTrue(!lockedIds.equals(currentIds), "product.unit.references-changed", "单位引用已变化，请重新提交");
        return unit;
    }

    private void checkName(String name, Long excludedId) {
        LambdaQueryWrapper<ProductUnit> query = Wrappers.lambdaQuery();
        query.eq(ProductUnit::getName, name);
        query.ne(excludedId != null, ProductUnit::getId, excludedId);
        boolean exists = super.baseMapper.exists(query);
        Assert.isTrue(exists, "product.unit.name-exists", "单位名称已存在");
    }
}
