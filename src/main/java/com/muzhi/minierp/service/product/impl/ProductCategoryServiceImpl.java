package com.muzhi.minierp.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.Product;
import com.muzhi.minierp.entity.product.ProductCategory;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.product.ProductCategoryMapper;
import com.muzhi.minierp.mapper.product.ProductMapper;
import com.muzhi.minierp.service.product.IProductCategoryService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 内部商品类目 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements IProductCategoryService {

    private final ProductMapper productMapper;

    @Override
    public List<ProductCategoryVO> tree() {
        LambdaQueryWrapper<ProductCategory> query = Wrappers.lambdaQuery();
        query.orderByAsc(ProductCategory::getSortOrder, ProductCategory::getId);
        List<ProductCategory> categories = super.baseMapper.selectList(query);
        List<ProductCategoryVO> nodes = BeanConvertUtils.mapList(categories, ProductCategoryVO.class);
        Map<Long, ProductCategoryVO> nodesById = new HashMap<>();
        for (ProductCategoryVO node : nodes) {
            node.setChildren(new ArrayList<>());
            nodesById.put(node.getId(), node);
        }

        List<ProductCategoryVO> roots = new ArrayList<>();
        for (ProductCategoryVO node : nodes) {
            if (node.getParentId().equals(0L)) {
                roots.add(node);
            } else {
                ProductCategoryVO parent = nodesById.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategory create(ProductCategory query) {
        Long parentId = query.getParentId() == null ? 0L : query.getParentId();
        this.validateParent(parentId);
        String code = query.getCode().trim();
        boolean codeExisted = super.baseMapper.existsByCode(code);
        Assert.isTrue(codeExisted, "product.category.code-already-exists", "商品分类编码已存在，删除后也不能复用");
        String name = query.getName().trim();
        this.validateSiblingName(parentId, name, null);

        // 仅复制允许写入的字段，主键及审计信息由服务端维护。
        ProductCategory category = new ProductCategory();
        Long id = IdWorker.getId();
        category.setId(id);
        category.setParentId(parentId);
        category.setCode(code);
        category.setName(name);
        int defaultSortOrder = 0;
        Integer sortOrder = query.getSortOrder() == null ? defaultSortOrder : query.getSortOrder();
        category.setSortOrder(sortOrder);
        String remark = StringUtils.trimToNull(query.getRemark());
        category.setRemark(remark);
        category.setIsDeleted(false);
        try {
            int inserted = super.baseMapper.insert(category);
            Assert.isTrue(inserted != 1, "product.category.create-failed", "商品分类创建失败");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("product.category.duplicate", "商品分类编码或同级名称已存在", exception);
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategory updateCategory(ProductCategory query) {
        ProductCategory category = this.requireCategory(query.getId());
        String name = query.getName().trim();
        this.validateSiblingName(category.getParentId(), name, category.getId());
        boolean nameChanged = !category.getName().equals(name);

        ProductCategory changes = new ProductCategory();
        changes.setName(name);
        changes.setSortOrder(query.getSortOrder());
        String remark = StringUtils.trimToNull(query.getRemark());
        LambdaUpdateWrapper<ProductCategory> update = Wrappers.lambdaUpdate();
        update.eq(ProductCategory::getId, category.getId());
        update.set(ProductCategory::getRemark, remark);
        try {
            int updated = super.baseMapper.update(changes, update);
            Assert.isTrue(updated != 1, "product.category.update-failed", "商品分类修改失败");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("product.category.name-already-exists", "同一父分类下已存在此名称", exception);
        }
        if (nameChanged) {
            List<Long> subtreeIds = super.baseMapper.selectSubtreeIds(category.getId());
            Product audit = new Product();
            productMapper.incrementVersionsByCategoryIds(subtreeIds, audit);
        }
        return this.requireCategory(category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        this.requireCategory(id);
        LambdaQueryWrapper<ProductCategory> childQuery = Wrappers.lambdaQuery();
        childQuery.eq(ProductCategory::getParentId, id);
        boolean hasChildren = super.baseMapper.exists(childQuery);
        Assert.isTrue(hasChildren, "product.category.has-children", "商品分类存在下级分类，不能删除");

        LambdaQueryWrapper<Product> productQuery = Wrappers.lambdaQuery();
        productQuery.eq(Product::getCategoryId, id);
        boolean hasProducts = productMapper.exists(productQuery);
        Assert.isTrue(hasProducts, "product.category.has-products", "商品分类存在商品，不能删除");
        ProductCategory category = new ProductCategory();
        category.setId(id);
        int deleted = super.baseMapper.deleteById(category);
        Assert.isTrue(deleted != 1, "product.category.delete-failed", "商品分类删除失败");
    }

    private ProductCategory requireCategory(Long id) {
        ProductCategory category = super.baseMapper.selectById(id);
        Assert.isNull(category, "product.category.not-found", "商品分类不存在");
        return category;
    }

    /**
     * 校验父级链有效且新分类不超过三级，顶级分类为第 1 级。
     */
    private void validateParent(Long parentId) {
        int maximumDepth = 3;
        int categoryDepth = 1;
        Long ancestorId = parentId;
        while (!ancestorId.equals(0L)) {
            ProductCategory parent = super.baseMapper.selectById(ancestorId);
            Assert.isNull(parent, "product.category.parent-not-found", "父分类不存在或已删除");
            categoryDepth++;
            Assert.isTrue(
                    categoryDepth > maximumDepth,
                    "product.category.depth-exceeded",
                    "商品分类最多支持{0}级",
                    maximumDepth
            );
            ancestorId = parent.getParentId();
        }
    }

    private void validateSiblingName(Long parentId, String name, Long excludedId) {
        LambdaQueryWrapper<ProductCategory> nameQuery = Wrappers.lambdaQuery();
        nameQuery.eq(ProductCategory::getParentId, parentId);
        nameQuery.eq(ProductCategory::getName, name);
        nameQuery.ne(excludedId != null, ProductCategory::getId, excludedId);
        boolean nameExisted = super.baseMapper.exists(nameQuery);
        Assert.isTrue(nameExisted, "product.category.name-already-exists", "同一父分类下已存在此名称");
    }
}
