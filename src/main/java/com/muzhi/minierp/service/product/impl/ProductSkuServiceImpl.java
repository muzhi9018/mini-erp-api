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
import com.muzhi.minierp.service.product.IProductImageService;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * <p>
 * 商品 SKU、选品和人工库存服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements IProductSkuService {

    private final ProductMapper productMapper;

    private final ProductSpecMapper productSpecMapper;

    private final ProductSpecValueMapper productSpecValueMapper;

    private final ProductSkuSpecValueMapper productSkuSpecValueMapper;

    private final ProductSnapshotMapper productSnapshotMapper;

    private final ProductCategoryMapper productCategoryMapper;

    private final IProductImageService productImageService;

    private final IProductUnitService productUnitService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void initialize(Long productId, List<ProductSkuVO> skus, List<ProductSpecVO> specs) {
        Map<String, ProductSpecVO> specsByKey = new HashMap<>();
        for (ProductSpecVO spec : specs) {
            specsByKey.put(spec.getClientKey(), spec);
        }
        for (ProductSkuVO sku : skus) {
            List<ProductSkuSpecValueVO> selections = new ArrayList<>();
            for (ProductRequests.ClientSelection input : sku.getSelections()) {
                ProductSpecVO spec = specsByKey.get(input.specClientKey());
                Assert.isNull(spec, "product.sku.spec-invalid", "SKU 选择了未知规格维度");
                ProductSpecValueVO value = spec.getValues().stream()
                        .filter(candidate -> candidate.getClientKey().equals(input.valueClientKey()))
                        .findFirst().orElse(null);
                Assert.isNull(value, "product.sku.value-invalid", "SKU 选择了不属于该维度的候选值");
                ProductSkuSpecValueVO selection = new ProductSkuSpecValueVO();
                selection.setSpecId(spec.getId());
                selection.setSpecValueId(value.getId());
                selections.add(selection);
            }
            this.insertSku(productId, sku, selections);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSkuVO> create(ProductVO query) {
        Product product = this.lockProduct(query.getId());
        boolean versionConflict = !Objects.equals(product.getVersion(), query.getExpectedVersion());
        Assert.isTrue(versionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        List<Long> createdIds = new ArrayList<>();
        for (ProductSkuVO sku : query.getSkus()) {
            List<ProductSkuSpecValueVO> selections = sku.getSpecValues();
            ProductSku created = this.insertSku(product.getId(), sku, selections);
            createdIds.add(created.getId());
        }
        this.incrementProductVersion(product);
        List<ProductSkuVO> all = this.listByProduct(product.getId());
        return all.stream().filter(sku -> createdIds.contains(sku.getId())).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.SkuResult updateSku(ProductSkuVO query) {
        ProductSku source = this.findSku(query.getId());
        Product product = this.lockProduct(source.getProductId());
        ProductSku sku = this.lockSku(source.getId(), query.getExpectedVersion());
        ProductUnit unit = productUnitService.requireUnit(query.getUnitId());
        boolean unitChanged = !sku.getUnitId().equals(unit.getId());
        if (unitChanged) {
            Assert.isTrue(sku.getStockQuantity().signum() != 0, "product.sku.unit-has-stock", "库存不为0时不能修改基本单位");
            LambdaQueryWrapper<ProductSnapshot> snapshots = Wrappers.lambdaQuery();
            snapshots.eq(ProductSnapshot::getSourceSkuId, sku.getId());
            boolean used = productSnapshotMapper.exists(snapshots);
            Assert.isTrue(used, "product.sku.unit-in-use", "已有业务使用的 SKU 不能修改基本单位");
        }
        LambdaUpdateWrapper<ProductSku> update = this.versionedUpdate(sku);
        update.set(ProductSku::getModel, StringUtils.trimToNull(query.getModel()));
        update.set(ProductSku::getUnitId, unit.getId());
        Integer quantityScale = query.getQuantityScale();
        if (quantityScale != null) {
            this.checkQuantity(sku.getStockQuantity(), quantityScale);
            update.set(ProductSku::getQuantityScale, quantityScale);
        }
        update.set(ProductSku::getReferencePurchasePrice, query.getReferencePurchasePrice());
        update.set(ProductSku::getReferencePurchaseCurrency, query.getReferencePurchaseCurrency());
        update.set(ProductSku::getReferenceSalePrice, query.getReferenceSalePrice());
        update.set(ProductSku::getReferenceSaleCurrency, query.getReferenceSaleCurrency());
        if (query.getSortOrder() != null) {
            update.set(ProductSku::getSortOrder, query.getSortOrder());
        }
        return this.finishUpdate(product, sku, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.SkuImagesResult saveImages(ProductSkuVO query) {
        ProductSku source = this.findSku(query.getId());
        Product product = this.lockProduct(source.getProductId());
        ProductSku sku = this.lockSku(source.getId(), query.getExpectedVersion());
        productImageService.replace(ProductEnum.ImageType.SKU, sku.getId(), query.getImages());

        LambdaUpdateWrapper<ProductSku> update = this.versionedUpdate(sku);
        ProductRequests.SkuResult versions = this.finishUpdate(product, sku, update);
        List<Long> skuIds = List.of(sku.getId());
        List<ProductImageVO> images = productImageService.listBySkus(skuIds);
        return new ProductRequests.SkuImagesResult(sku.getId(), versions.skuVersion(), versions.productVersion(), images);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.SkuResult changeStatus(ProductSkuVO query) {
        ProductSku source = this.findSku(query.getId());
        Product product = this.lockProduct(source.getProductId());
        ProductSku sku = this.lockSku(source.getId(), query.getExpectedVersion());
        LambdaUpdateWrapper<ProductSku> update = this.versionedUpdate(sku);
        update.set(ProductSku::getStatus, query.getStatus());
        return this.finishUpdate(product, sku, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.SkuResult setStock(ProductSkuVO query) {
        ProductSku source = this.findSku(query.getId());
        Product product = this.lockProduct(source.getProductId());
        ProductSku sku = this.lockSku(source.getId(), query.getExpectedVersion());
        this.checkQuantity(query.getStockQuantity(), sku.getQuantityScale());
        LambdaUpdateWrapper<ProductSku> update = this.versionedUpdate(sku);
        update.set(ProductSku::getStockQuantity, query.getStockQuantity());
        sku.setStockQuantity(query.getStockQuantity());
        return this.finishUpdate(product, sku, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductRequests.SkuResult delete(ProductRequests.VersionedId query) {
        ProductSku source = this.findSku(query.id());
        Product product = this.lockProduct(source.getProductId());
        ProductSku sku = this.lockSku(source.getId(), query.expectedVersion());
        Assert.isTrue(sku.getStockQuantity().signum() != 0, "product.sku.has-stock", "SKU 库存不为0，不能删除");
        LambdaUpdateWrapper<ProductSku> update = this.versionedUpdate(sku);
        update.set(ProductSku::getIsDeleted, true);
        LambdaQueryWrapper<ProductSkuSpecValue> selections = Wrappers.lambdaQuery();
        selections.eq(ProductSkuSpecValue::getSkuId, sku.getId());
        productSkuSpecValueMapper.delete(selections);
        productImageService.replace(ProductEnum.ImageType.SKU, sku.getId(), List.of());
        return this.finishUpdate(product, sku, update);
    }

    @Override
    public IPage<ProductSkuVO> list(ProductQueryVO query, Integer pageNum, Integer pageSize) {
        return this.queryPage(query, pageNum, pageSize, false, false, null);
    }

    @Override
    public IPage<ProductOptionVO> options(ProductQueryVO query, Integer pageNum, Integer pageSize) {
        IPage<ProductSkuVO> page = this.queryPage(query, pageNum, pageSize, true, false, null);
        return this.toOptionPage(page, pageNum, pageSize);
    }

    @Override
    public IPage<ProductOptionVO> quotationOptions(ProductQuotationQueryVO query, Integer pageNum, Integer pageSize) {
        String currency = query.getReferenceSaleCurrency();
        IPage<ProductSkuVO> page = this.queryPage(query, pageNum, pageSize, true, true, currency);
        return this.toOptionPage(page, pageNum, pageSize);
    }

    @Override
    public List<ProductOptionVO> optionsByIds(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        List<ProductSkuVO> skus = super.baseMapper.selectOptionsByIds(skuIds);
        this.enrich(skus);
        return this.toOptions(skus);
    }

    /**
     * 使用与管理选品相同的规格和展示字段组装报价选品数据。
     */
    private List<ProductOptionVO> toOptions(List<ProductSkuVO> skus) {
        List<ProductOptionVO> options = BeanConvertUtils.mapList(skus, ProductOptionVO.class);
        for (int index = 0; index < options.size(); index++) {
            ProductOptionVO option = options.get(index);
            ProductSkuVO sku = skus.get(index);
            option.setSkuId(sku.getId());
            option.setSkuVersion(sku.getVersion());
            List<ProductOptionSpecVO> specs = BeanConvertUtils.mapList(sku.getSpecValues(), ProductOptionSpecVO.class);
            option.setSpecs(specs);
        }
        return options;
    }

    private IPage<ProductOptionVO> toOptionPage(IPage<ProductSkuVO> page, Integer pageNum, Integer pageSize) {
        List<ProductOptionVO> options = this.toOptions(page.getRecords());
        Page<ProductOptionVO> result = new Page<>(pageNum, pageSize, page.getTotal());
        result.setRecords(options);
        return result;
    }

    @Override
    public List<ProductSkuVO> listByProduct(Long productId) {
        ProductQueryVO query = new ProductQueryVO();
        query.setProductId(productId);
        Page<ProductSkuVO> page = new Page<>(1, -1);
        IPage<ProductSkuVO> result = super.baseMapper.selectSkuPage(page, query, null, false, false, null);
        this.enrich(result.getRecords());
        return result.getRecords();
    }

    @Override
    public List<List<ProductSkuSpecValue>> preview(ProductRequests.Preview query) {
        Product product = productMapper.selectById(query.productId());
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        List<ProductSpec> specs = this.findSpecs(query.productId());
        Assert.isTrue(specs.size() != query.dimensions().size(), "product.sku.spec-incomplete", "必须为每个规格维度选择候选值");
        List<List<ProductSkuSpecValue>> combinations = new ArrayList<>();
        combinations.add(new ArrayList<>());
        for (ProductRequests.PreviewDimension dimension : query.dimensions()) {
            boolean belongs = specs.stream().anyMatch(spec -> spec.getId().equals(dimension.specId()));
            Assert.isTrue(!belongs, "product.sku.spec-invalid", "规格维度不属于当前商品");
            List<List<ProductSkuSpecValue>> next = new ArrayList<>();
            for (Long valueId : dimension.specValueIds()) {
                this.checkValueBelongs(dimension.specId(), valueId);
                for (List<ProductSkuSpecValue> combination : combinations) {
                    List<ProductSkuSpecValue> expanded = new ArrayList<>(combination);
                    ProductSkuSpecValue selection = new ProductSkuSpecValue();
                    selection.setSpecId(dimension.specId());
                    selection.setSpecValueId(valueId);
                    expanded.add(selection);
                    next.add(expanded);
                }
            }
            combinations = next;
        }
        return combinations;
    }

    /**
     * 保存 SKU 及其已解析的规格选择，主键、归属和版本由服务端生成。
     */
    private ProductSku insertSku(Long productId, ProductSkuVO query, List<ProductSkuSpecValueVO> selections) {
        String code = query.getCode().trim();
        boolean codeExists = super.baseMapper.existsByCode(code);
        Assert.isTrue(codeExists, "product.sku.code-exists", "SKU 货号已存在，删除后也不能复用");
        List<ProductSpec> specs = this.findSpecs(productId);
        Assert.isTrue(specs.size() != selections.size(), "product.sku.spec-incomplete", "每个 SKU 必须且只能选择每个维度的一个候选值");
        Set<Long> selectedSpecs = new HashSet<>();
        for (ProductSkuSpecValueVO selection : selections) {
            boolean belongs = specs.stream().anyMatch(spec -> spec.getId().equals(selection.getSpecId()));
            boolean duplicate = !selectedSpecs.add(selection.getSpecId());
            Assert.isTrue(!belongs || duplicate, "product.sku.spec-invalid", "规格维度重复或不属于当前商品");
            this.checkValueBelongs(selection.getSpecId(), selection.getSpecValueId());
        }
        String key = this.specificationKey(selections);
        LambdaQueryWrapper<ProductSku> sameCombination = Wrappers.lambdaQuery();
        sameCombination.eq(ProductSku::getProductId, productId);
        sameCombination.eq(ProductSku::getSpecificationKey, key);
        boolean combinationExists = super.baseMapper.exists(sameCombination);
        Assert.isTrue(combinationExists, "product.sku.combination-exists", "SKU 规格组合已存在，停用的 SKU 仍占用该组合");
        ProductUnit unit = productUnitService.requireUnit(query.getUnitId());
        BigDecimal stock = query.getStockQuantity() == null ? BigDecimal.ZERO : query.getStockQuantity();
        Integer requestedScale = query.getQuantityScale();
        int quantityScale = requestedScale == null ? 0 : requestedScale;
        this.checkQuantity(stock, quantityScale);
        ProductSku sku = new ProductSku();
        Long id = IdWorker.getId();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setCode(code);
        sku.setModel(StringUtils.trimToNull(query.getModel()));
        sku.setUnitId(unit.getId());
        sku.setQuantityScale(quantityScale);
        sku.setReferencePurchasePrice(query.getReferencePurchasePrice());
        sku.setReferencePurchaseCurrency(query.getReferencePurchaseCurrency());
        sku.setReferenceSalePrice(query.getReferenceSalePrice());
        sku.setReferenceSaleCurrency(query.getReferenceSaleCurrency());
        sku.setStockQuantity(stock);
        sku.setSpecificationKey(key);
        sku.setStatus(query.getStatus());
        sku.setSortOrder(query.getSortOrder() == null ? 0 : query.getSortOrder());
        sku.setVersion(0L);
        sku.setIsDeleted(false);
        try {
            super.baseMapper.insert(sku);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("product.sku.duplicate", "SKU 货号或规格组合已存在", e);
        }
        for (ProductSkuSpecValueVO input : selections) {
            ProductSkuSpecValue selection = new ProductSkuSpecValue();
            Long selectionId = IdWorker.getId();
            selection.setId(selectionId);
            selection.setSkuId(sku.getId());
            selection.setSpecId(input.getSpecId());
            selection.setSpecValueId(input.getSpecValueId());
            selection.setIsDeleted(false);
            productSkuSpecValueMapper.insert(selection);
        }
        List<ProductImageVO> images = query.getImages();
        if (images != null && !images.isEmpty()) {
            productImageService.replace(ProductEnum.ImageType.SKU, sku.getId(), images);
        }
        return sku;
    }

    private List<ProductSpec> findSpecs(Long productId) {
        LambdaQueryWrapper<ProductSpec> query = Wrappers.lambdaQuery();
        query.eq(ProductSpec::getProductId, productId);
        query.orderByAsc(ProductSpec::getSortOrder, ProductSpec::getId);
        return productSpecMapper.selectList(query);
    }

    private void checkValueBelongs(Long specId, Long valueId) {
        ProductSpecValue value = productSpecValueMapper.selectById(valueId);
        Assert.isTrue(value == null || !value.getSpecId().equals(specId),
                "product.sku.value-invalid", "候选值不存在或不属于所选维度");
    }

    private String specificationKey(List<ProductSkuSpecValueVO> selections) {
        List<ProductSkuSpecValueVO> sorted = new ArrayList<>(selections);
        sorted.sort(Comparator.comparing(ProductSkuSpecValueVO::getSpecId));
        StringJoiner canonical = new StringJoiner(";", "[", "]");
        for (ProductSkuSpecValueVO selection : sorted) {
            canonical.add(selection.getSpecId() + ":" + selection.getSpecValueId());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = canonical.toString().getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }

    private LambdaUpdateWrapper<ProductSku> versionedUpdate(ProductSku sku) {
        LambdaUpdateWrapper<ProductSku> update = Wrappers.lambdaUpdate();
        update.eq(ProductSku::getId, sku.getId());
        update.eq(ProductSku::getVersion, sku.getVersion());
        Long nextVersion = Math.incrementExact(sku.getVersion());
        update.set(ProductSku::getVersion, nextVersion);
        return update;
    }

    private ProductRequests.SkuResult finishUpdate(Product product, ProductSku sku, LambdaUpdateWrapper<ProductSku> update) {
        int changed = super.baseMapper.update(new ProductSku(), update);
        Assert.isTrue(changed != 1, "product.version-conflict", "资料已更新，请重新读取后确认");
        Long productVersion = this.incrementProductVersion(product);
        Long skuVersion = Math.incrementExact(sku.getVersion());
        return new ProductRequests.SkuResult(sku.getId(), sku.getStockQuantity(), skuVersion, productVersion);
    }

    private IPage<ProductSkuVO> queryPage(
            ProductQueryVO query, Integer pageNum, Integer pageSize, boolean options, boolean quotation, String currency
    ) {
        List<Long> categoryIds = null;
        if (query.getCategoryId() != null) {
            categoryIds = productCategoryMapper.selectSubtreeIds(query.getCategoryId());
            if (categoryIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
        }
        String keyword = StringUtils.trimToNull(query.getKeyword());
        query.setKeyword(keyword);
        Page<ProductSkuVO> page = new Page<>(pageNum, pageSize);
        IPage<ProductSkuVO> result = super.baseMapper.selectSkuPage(page, query, categoryIds, options, quotation, currency);
        this.enrich(result.getRecords());
        return result;
    }

    /**
     * 批量装载 SKU 的规格及其自身图片。
     */
    private void enrich(List<ProductSkuVO> skus) {
        if (skus.isEmpty()) {
            return;
        }
        List<Long> ids = skus.stream().map(ProductSku::getId).toList();
        List<ProductSkuSpecValueVO> selections = productSkuSpecValueMapper.selectDetails(ids);
        Map<Long, List<ProductSkuSpecValueVO>> bySku = new HashMap<>();
        for (ProductSkuSpecValueVO selection : selections) {
            bySku.computeIfAbsent(selection.getSkuId(), ignored -> new ArrayList<>()).add(selection);
        }
        List<ProductImageVO> images = productImageService.listBySkus(ids);
        Map<Long, List<ProductImageVO>> imagesBySku = new HashMap<>();
        for (ProductImageVO image : images) {
            imagesBySku.computeIfAbsent(image.getOwnerId(), ignored -> new ArrayList<>()).add(image);
        }
        for (ProductSkuVO sku : skus) {
            sku.setImages(imagesBySku.getOrDefault(sku.getId(), List.of()));
            List<ProductSkuSpecValueVO> values = bySku.getOrDefault(sku.getId(), List.of());
            sku.setSpecValues(values);
            StringJoiner text = new StringJoiner(" / ");
            for (ProductSkuSpecValueVO selection : values) {
                String value = selection.getTextValue();
                if (selection.getNumberValue() != null) {
                    value = selection.getNumberValue().stripTrailingZeros().toPlainString();
                }
                String unit = selection.getUnitName() == null ? "" : selection.getUnitName();
                text.add(selection.getSpecName() + ":" + value + unit);
            }
            sku.setSpecificationText(text.toString());
        }
    }

    /**
     * 锁定所属商品，后续 SKU 加锁及修改始终在同一事务内执行。
     */
    private Product lockProduct(Long productId) {
        Product product = productMapper.selectForUpdate(productId);
        Assert.isNull(product, "product.not-found", "商品不存在或已删除");
        return product;
    }

    /**
     * 根据不可变的商品归属定位 SKU，供后续先锁商品再锁 SKU。
     */
    private ProductSku findSku(Long skuId) {
        ProductSku sku = super.baseMapper.selectById(skuId);
        Assert.isNull(sku, "product.sku.not-found", "SKU 不存在或已删除");
        return sku;
    }

    /**
     * 已锁定所属商品后，锁定 SKU 并校验客户端确认的行版本。
     */
    private ProductSku lockSku(Long skuId, Long expectedVersion) {
        ProductSku sku = super.baseMapper.selectForUpdate(skuId);
        Assert.isNull(sku, "product.sku.not-found", "SKU 不存在或已删除");
        boolean versionConflict = !Objects.equals(sku.getVersion(), expectedVersion);
        Assert.isTrue(versionConflict, "product.version-conflict", "资料已更新，请重新读取后确认");
        return sku;
    }

    /**
     * 校验库存是否符合 SKU 的数量小数位，仅忽略末尾多余的零。
     */
    private void checkQuantity(BigDecimal quantity, int quantityScale) {
        BigDecimal normalized = quantity.stripTrailingZeros();
        Assert.isTrue(normalized.scale() > quantityScale, "product.sku.quantity-scale", "库存数量超出 SKU 允许的小数位数");
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
