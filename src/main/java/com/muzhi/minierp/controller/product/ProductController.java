package com.muzhi.minierp.controller.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.product.*;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.product.*;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 商品档案前端控制器，集中维护商品、图片、规格及所属 SKU
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    private final IProductImageService productImageService;

    private final IProductSpecService productSpecService;

    private final IProductSkuService productSkuService;

    /**
     * 分页查询商品，SKU 关键字命中后按商品去重。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('product-manager:product:query')")
    public JsonResult<IPage<ProductVO>> list(ProductQueryVO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        this.validateQuery(query, pageNum, pageSize);
        IPage<ProductVO> result = productService.list(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    /**
     * 查询商品完整管理详情。
     */
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('product-manager:product:query')")
    public JsonResult<ProductVO> detail(@RequestParam Long productId) {
        this.validateId(productId);
        ProductVO result = productService.detail(productId);
        return JsonResult.success(result);
    }

    /**
     * 一次保存商品、图片、结构化规格和至少一条 SKU。
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('product-manager:product:create')")
    public JsonResult<ProductVO> create(@RequestBody ProductVO query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        this.validateProduct(query);
        this.validateText(query.getCode(), 64, true);
        this.validateStatus(query.getStatus());
        if (query.getImages() == null) {
            query.setImages(List.of());
        }
        this.validateImages(query.getImages());
        if (query.getSpecs() == null) {
            query.setSpecs(List.of());
        }
        Map<String, Set<String>> valuesBySpec = new HashMap<>();
        Set<String> names = new HashSet<>();
        for (ProductSpecVO spec : query.getSpecs()) {
            this.validateSpec(spec, true);
            this.validateText(spec.getClientKey(), 128, true);
            boolean duplicateKey = valuesBySpec.containsKey(spec.getClientKey());
            boolean duplicateName = !names.add(spec.getName().trim());
            Assert.isTrue(duplicateKey || duplicateName, "product.spec.duplicate", "规格临时键或维度名称不能重复");
            Set<String> valueKeys = new HashSet<>();
            for (ProductSpecValueVO value : spec.getValues()) {
                this.validateText(value.getClientKey(), 128, true);
                boolean duplicate = !valueKeys.add(value.getClientKey());
                Assert.isTrue(duplicate, "product.spec.client-key-duplicate", "同一维度的候选值临时键不能重复");
            }
            valuesBySpec.put(spec.getClientKey(), valueKeys);
        }
        Assert.isEmpty(query.getSkus(), "product.sku.list-required", "创建商品时至少提交一条 SKU");
        Set<String> codes = new HashSet<>();
        Set<Set<ProductRequests.ClientSelection>> combinations = new HashSet<>();
        for (ProductSkuVO sku : query.getSkus()) {
            this.validateSku(sku, true);
            if (sku.getSelections() == null) {
                sku.setSelections(List.of());
            }
            Assert.isTrue(sku.getSelections().size() != valuesBySpec.size(),
                    "product.sku.spec-incomplete", "SKU 必须选择全部规格维度");
            Set<String> chosen = new HashSet<>();
            for (ProductRequests.ClientSelection selection : sku.getSelections()) {
                Assert.isNull(selection, "product.sku.selection-required", "规格选择项不能为空");
                Set<String> values = valuesBySpec.get(selection.specClientKey());
                boolean duplicate = !chosen.add(selection.specClientKey());
                boolean invalid = values == null || !values.contains(selection.valueClientKey());
                Assert.isTrue(duplicate || invalid, "product.sku.selection-invalid", "规格临时键重复或候选值引用不完整");
            }
            Set<ProductRequests.ClientSelection> combination = new HashSet<>(sku.getSelections());
            boolean duplicate = !codes.add(sku.getCode().trim()) || !combinations.add(combination);
            Assert.isTrue(duplicate, "product.sku.duplicate", "SKU 货号或规格组合不能重复");
        }
        ProductVO result = productService.create(query);
        return JsonResult.success(result);
    }

    /**
     * 普通资料编辑，不接受图片或库存的写入语义。
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<Product> update(@RequestBody ProductVO query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        this.validateProduct(query);
        Long id = this.resolveId(query.getId(), query.getProductId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        Product result = productService.updateProduct(query);
        return JsonResult.success(result);
    }

    /**
     * 保存完整目标图片列表，未指定主图时自动使用第一张。
     */
    @PostMapping("/images/save")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductRequests.ImagesResult> saveImages(@RequestBody ProductVO query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        Long id = this.resolveId(query.getId(), query.getProductId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        this.validateImages(query.getImages());
        ProductRequests.ImagesResult result = productImageService.saveImages(query);
        return JsonResult.success(result);
    }

    /**
     * 修改商品启停状态，不改写各 SKU 的状态。
     */
    @PostMapping("/change-status")
    @PreAuthorize("hasAuthority('product-manager:product:change-status')")
    public JsonResult<Product> changeStatus(@RequestBody ProductVO query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        Long id = this.resolveId(query.getId(), query.getProductId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        this.validateStatus(query.getStatus());
        Product result = productService.changeStatus(query);
        return JsonResult.success(result);
    }

    /**
     * 删除满足库存限制的商品和附属主档，保留全部快照。
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('product-manager:product:delete')")
    public JsonResult<Long> delete(@RequestBody ProductRequests.VersionedId query) {
        this.validateVersionedId(query);
        Long version = productService.delete(query);
        return JsonResult.success(version);
    }

    /**
     * 未建立 SKU 时添加规格维度及候选值。
     */
    @PostMapping("/spec/create")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductSpecVO> createSpec(@RequestBody ProductSpecVO query) {
        this.validateSpec(query, true);
        this.validateId(query.getProductId());
        this.validateVersion(query.getExpectedVersion());
        ProductSpecVO result = productSpecService.create(query);
        return JsonResult.success(result);
    }

    /**
     * 修正规格维度的显示名称和排序。
     */
    @PostMapping("/spec/update")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductSpecVO> updateSpec(@RequestBody ProductSpecVO query) {
        this.validateSpec(query, false);
        this.validateId(query.getId());
        this.validateVersion(query.getExpectedVersion());
        ProductSpecVO result = productSpecService.updateSpec(query);
        return JsonResult.success(result);
    }

    /**
     * 删除无 SKU 引用的维度，期望版本为所属商品版本。
     */
    @PostMapping("/spec/delete")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<Void> deleteSpec(@RequestBody ProductRequests.VersionedId query) {
        this.validateVersionedId(query);
        productSpecService.deleteSpec(query);
        return JsonResult.success();
    }

    /**
     * 添加已有维度的候选值。
     */
    @PostMapping("/spec/value/create")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductSpecValueVO> createValue(@RequestBody ProductSpecValueVO query) {
        this.validateValue(query, true);
        this.validateId(query.getSpecId());
        this.validateVersion(query.getExpectedVersion());
        ProductSpecValueVO result = productSpecService.createValue(query);
        return JsonResult.success(result);
    }

    /**
     * 修改未被使用的候选值；只传 ID、版本和排序可仅调整排序。
     */
    @PostMapping("/spec/value/update")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductSpecValueVO> updateValue(@RequestBody ProductSpecValueVO query) {
        this.validateValue(query, false);
        this.validateId(query.getId());
        this.validateVersion(query.getExpectedVersion());
        ProductSpecValueVO result = productSpecService.updateValue(query);
        return JsonResult.success(result);
    }

    /**
     * 删除无有效 SKU 引用的候选值。
     */
    @PostMapping("/spec/value/delete")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<Void> deleteValue(@RequestBody ProductRequests.VersionedId query) {
        this.validateVersionedId(query);
        productSpecService.deleteValue(query);
        return JsonResult.success();
    }

    /**
     * 分页查询 SKU 管理资料。
     */
    @GetMapping("/sku/list")
    @PreAuthorize("hasAuthority('product-manager:product:query')")
    public JsonResult<IPage<ProductSkuVO>> skuList(ProductQueryVO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        this.validateQuery(query, pageNum, pageSize);
        IPage<ProductSkuVO> result = productSkuService.list(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    /**
     * 业务选品只返回启用商品和启用 SKU，不包含进价及审计字段。
     */
    @GetMapping("/sku/options")
    @PreAuthorize("hasAuthority('product-manager:product:sku:options')")
    public JsonResult<IPage<ProductOptionVO>> options(ProductQueryVO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        this.validateQuery(query, pageNum, pageSize);
        IPage<ProductOptionVO> result = productSkuService.options(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    /**
     * 批量创建实际经营的组合，所有项原子提交。
     */
    @PostMapping("/sku/create")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<List<ProductSkuVO>> createSkus(@RequestBody ProductVO query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        Long id = this.resolveId(query.getId(), query.getProductId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        Assert.isEmpty(query.getSkus(), "product.sku.list-required", "至少提交一条 SKU");
        Set<String> codes = new HashSet<>();
        Set<Set<String>> combinations = new HashSet<>();
        for (ProductSkuVO sku : query.getSkus()) {
            this.validateSku(sku, true);
            if (sku.getSpecValues() == null) {
                sku.setSpecValues(List.of());
            }
            Set<Long> specIds = new HashSet<>();
            Set<String> combination = new HashSet<>();
            for (ProductSkuSpecValueVO selection : sku.getSpecValues()) {
                Assert.isNull(selection, "product.sku.selection-required", "规格选择项不能为空");
                this.validateId(selection.getSpecId());
                this.validateId(selection.getSpecValueId());
                boolean duplicate = !specIds.add(selection.getSpecId());
                Assert.isTrue(duplicate, "product.sku.spec-duplicate", "同一 SKU 的规格维度不能重复");
                combination.add(selection.getSpecId() + ":" + selection.getSpecValueId());
            }
            boolean duplicate = !codes.add(sku.getCode().trim()) || !combinations.add(combination);
            Assert.isTrue(duplicate, "product.sku.duplicate", "SKU 货号或规格组合不能重复");
        }
        List<ProductSkuVO> result = productSkuService.create(query);
        return JsonResult.success(result);
    }

    /**
     * 修改 SKU 型号、单位、数量小数位、参考价格和排序。
     */
    @PostMapping("/sku/update")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductRequests.SkuResult> updateSku(@RequestBody ProductSkuVO query) {
        this.validateSku(query, false);
        Long id = this.resolveId(query.getId(), query.getSkuId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        ProductRequests.SkuResult result = productSkuService.updateSku(query);
        return JsonResult.success(result);
    }

    /**
     * 保存 SKU 自身的完整图片列表，递增 SKU 行版本和商品版本。
     */
    @PostMapping("/sku/images/save")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<ProductRequests.SkuImagesResult> saveSkuImages(@RequestBody ProductSkuVO query) {
        Assert.isNull(query, "product.sku.required", "SKU 信息不能为空");
        this.validateId(query.getId());
        this.validateVersion(query.getExpectedVersion());
        this.validateImages(query.getImages());
        ProductRequests.SkuImagesResult result = productSkuService.saveImages(query);
        return JsonResult.success(result);
    }

    /**
     * 修改 SKU 启停状态。
     */
    @PostMapping("/sku/change-status")
    @PreAuthorize("hasAuthority('product-manager:product:change-status')")
    public JsonResult<ProductRequests.SkuResult> changeSkuStatus(@RequestBody ProductSkuVO query) {
        Assert.isNull(query, "product.sku.required", "SKU 信息不能为空");
        Long id = this.resolveId(query.getId(), query.getSkuId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        this.validateStatus(query.getStatus());
        ProductRequests.SkuResult result = productSkuService.changeStatus(query);
        return JsonResult.success(result);
    }

    /**
     * 设置库存绝对数量，独立权限且必须提交最新 SKU 行版本。
     */
    @PostMapping("/sku/stock/set")
    @PreAuthorize("hasAuthority('product-manager:product:stock:set')")
    public JsonResult<ProductRequests.SkuResult> setStock(@RequestBody ProductSkuVO query) {
        Assert.isNull(query, "product.sku.required", "SKU 信息不能为空");
        Long id = this.resolveId(query.getId(), query.getSkuId());
        query.setId(id);
        this.validateVersion(query.getExpectedVersion());
        this.validateNumber(query.getStockQuantity(), true, true);
        ProductRequests.SkuResult result = productSkuService.setStock(query);
        return JsonResult.success(result);
    }

    /**
     * 零库存 SKU 逻辑删除，保留历史快照。
     */
    @PostMapping("/sku/delete")
    @PreAuthorize("hasAuthority('product-manager:product:delete')")
    public JsonResult<ProductRequests.SkuResult> deleteSku(@RequestBody ProductRequests.VersionedId query) {
        this.validateVersionedId(query);
        ProductRequests.SkuResult result = productSkuService.delete(query);
        return JsonResult.success(result);
    }

    /**
     * 预览候选值的组合，不强制保存全部组合。
     */
    @PostMapping("/sku/preview")
    @PreAuthorize("hasAuthority('product-manager:product:update')")
    public JsonResult<List<List<ProductSkuSpecValue>>> preview(@RequestBody ProductRequests.Preview query) {
        Assert.isNull(query, "product.required", "商品信息不能为空");
        this.validateId(query.productId());
        Assert.isNull(query.dimensions(), "product.spec.list-required", "规格列表不能为空");
        Set<Long> specIds = new HashSet<>();
        long count = 1;
        int maximumCombinations = 1000;
        for (ProductRequests.PreviewDimension dimension : query.dimensions()) {
            Assert.isNull(dimension, "product.spec.required", "规格维度不能为空");
            this.validateId(dimension.specId());
            boolean duplicate = !specIds.add(dimension.specId());
            Assert.isTrue(duplicate, "product.spec.duplicate", "规格维度不能重复");
            Assert.isEmpty(dimension.specValueIds(), "product.spec.values-required", "候选值列表不能为空");
            Set<Long> valueIds = new HashSet<>();
            for (Long id : dimension.specValueIds()) {
                this.validateId(id);
                Assert.isTrue(!valueIds.add(id), "product.spec.value-duplicate", "候选值不能重复");
            }
            count *= valueIds.size();
            Assert.isTrue(count > maximumCombinations, "product.sku.preview-too-large", "单次预览组合不能超过{0}条", maximumCombinations);
        }
        List<List<ProductSkuSpecValue>> result = productSkuService.preview(query);
        return JsonResult.success(result);
    }

    private void validateProduct(Product query) {
        this.validateId(query.getCategoryId());
        this.validateText(query.getName(), 256, true);
        this.validateText(query.getBrand(), 128, false);
        this.validateText(query.getDescription(), 2048, false);
    }

    /**
     * 校验 SKU 资料，创建时同时校验初始库存和可选图片。
     */
    private void validateSku(ProductSkuVO query, boolean create) {
        Assert.isNull(query, "product.sku.required", "SKU 信息不能为空");
        if (create) {
            this.validateText(query.getCode(), 64, true);
            this.validateStatus(query.getStatus());
            this.validateNumber(query.getStockQuantity(), false, true);
            if (query.getImages() != null) {
                this.validateImages(query.getImages());
            }
        }
        this.validateText(query.getModel(), 128, false);
        this.validateId(query.getUnitId());
        Integer quantityScale = query.getQuantityScale();
        boolean invalidScale = quantityScale != null && (quantityScale < 0 || quantityScale > 6);
        Assert.isTrue(invalidScale, "product.sku.quantity-scale-invalid", "SKU 数量小数位数必须在 0 至 6 之间");
        this.validateReferencePrice(query.getReferencePurchasePrice(), query.getReferencePurchaseCurrency());
        this.validateReferencePrice(query.getReferenceSalePrice(), query.getReferenceSaleCurrency());
    }

    /**
     * 校验单组参考金额及币种，两者均可为空。
     */
    private void validateReferencePrice(BigDecimal amount, String currency) {
        boolean incomplete = (amount == null) != (currency == null);
        Assert.isTrue(incomplete, "product.sku.price-currency-required", "参考金额和币种必须同时填写或同时留空");
        if (currency != null) {
            CurrencyEnum value = CurrencyEnum.ofCode(currency);
            Assert.isNull(value, "product.sku.currency-invalid", "币种必须为 CNY 或 TWD");
        }
        this.validateNumber(amount, false, true);
    }

    private void validateSpec(ProductSpecVO query, boolean create) {
        Assert.isNull(query, "product.spec.required", "规格维度不能为空");
        this.validateText(query.getName(), 64, true);
        if (!create) {
            return;
        }
        ProductEnum.SpecValueType type = ProductEnum.SpecValueType.ofCode(query.getValueType());
        Assert.isNull(type, "product.spec.type-invalid", "规格类型必须为 TEXT 或 NUMBER");
        Assert.isTrue(type == ProductEnum.SpecValueType.TEXT && query.getUnitId() != null,
                "product.spec.text-unit-invalid", "文本规格不能设置计量单位");
        if (query.getUnitId() != null) {
            this.validateId(query.getUnitId());
        }
        Assert.isEmpty(query.getValues(), "product.spec.values-required", "规格至少需要一个候选值");
        Set<String> values = new HashSet<>();
        for (ProductSpecValueVO value : query.getValues()) {
            this.validateValue(value, true);
            boolean invalidType = type == ProductEnum.SpecValueType.TEXT ? value.getTextValue() == null : value.getNumberValue() == null;
            Assert.isTrue(invalidType, "product.spec.value-type-invalid", "候选值与维度类型不匹配");
            String normalized = value.getTextValue() == null
                    ? value.getNumberValue().stripTrailingZeros().toPlainString() : value.getTextValue().trim();
            Assert.isTrue(!values.add(normalized), "product.spec.value-duplicate", "候选值不能重复");
        }
    }

    private void validateValue(ProductSpecValue query, boolean required) {
        Assert.isNull(query, "product.spec.value-required", "候选值不能为空");
        boolean hasText = query.getTextValue() != null;
        boolean hasNumber = query.getNumberValue() != null;
        Assert.isTrue((hasText && hasNumber) || (required && !hasText && !hasNumber),
                "product.spec.value-invalid", "文本值与数值必须且只能填写一个");
        if (hasText) {
            this.validateText(query.getTextValue(), 128, true);
        }
        this.validateNumber(query.getNumberValue(), false, false);
    }

    /**
     * 校验同一商品或 SKU 的目标图片列表。
     */
    private void validateImages(List<ProductImageVO> images) {
        Assert.isNull(images, "product.images-required", "图片列表不能为空，清空时请传空列表");
        Set<Long> ids = new HashSet<>();
        int covers = 0;
        for (ProductImageVO image : images) {
            Assert.isNull(image, "product.image.required", "图片条目不能为空");
            this.validateId(image.getAttachmentId());
            Assert.isTrue(!ids.add(image.getAttachmentId()), "product.image.duplicate", "同一图片列表不能重复绑定附件");
            if (Boolean.TRUE.equals(image.getIsCover())) {
                covers++;
            }
        }
        Assert.isTrue(covers > 1, "product.image.multiple-covers", "只能指定一张主图");
    }

    private void validateQuery(ProductQueryVO query, Integer pageNum, Integer pageSize) {
        Assert.isTrue(pageNum == null || pageNum <= 0, "pagination.page-num-invalid", "当前页必须大于0");
        Assert.isTrue(pageSize == null || pageSize <= 0, "pagination.page-size-invalid", "每页数量必须大于0");
        if (query.getCategoryId() != null) {
            this.validateId(query.getCategoryId());
        }
        if (query.getProductId() != null) {
            this.validateId(query.getProductId());
        }
        if (query.getStatus() != null) {
            this.validateStatus(query.getStatus());
        }
        if (query.getSkuStatus() != null) {
            this.validateStatus(query.getSkuStatus());
        }
        if (query.getSpecs() == null) {
            query.setSpecs(List.of());
        }
        Set<Long> specIds = new HashSet<>();
        for (ProductQueryVO.SpecFilter filter : query.getSpecs()) {
            Assert.isNull(filter, "product.spec.filter-required", "规格筛选不能为空");
            this.validateId(filter.getSpecId());
            Assert.isTrue(!specIds.add(filter.getSpecId()), "product.spec.filter-duplicate", "同一维度不能重复筛选");
            boolean textFilter = filter.getSpecValueId() != null;
            boolean rangeFilter = filter.getMinimum() != null || filter.getMaximum() != null;
            Assert.isTrue(textFilter == rangeFilter, "product.spec.filter-invalid", "请选择候选值或数值区间其中一种筛选方式");
            if (textFilter) {
                this.validateId(filter.getSpecValueId());
            }
            this.validateNumber(filter.getMinimum(), false, false);
            this.validateNumber(filter.getMaximum(), false, false);
            boolean reversed = filter.getMinimum() != null && filter.getMaximum() != null && filter.getMinimum().compareTo(filter.getMaximum()) > 0;
            Assert.isTrue(reversed, "product.spec.range-invalid", "规格区间下限不能大于上限");
        }
    }

    private void validateVersionedId(ProductRequests.VersionedId query) {
        Assert.isNull(query, "product.operation-required", "操作参数不能为空");
        this.validateId(query.id());
        this.validateVersion(query.expectedVersion());
    }

    /**
     * 兼容既有请求的 ID 字段，两种字段同时提交时必须一致。
     */
    private Long resolveId(Long id, Long specificId) {
        boolean conflict = id != null && specificId != null && !id.equals(specificId);
        Assert.isTrue(conflict, "product.id-conflict", "请求中的业务 ID 必须一致");
        Long resolvedId = id == null ? specificId : id;
        this.validateId(resolvedId);
        return resolvedId;
    }

    private void validateId(Long id) {
        Assert.isTrue(id == null || id <= 0, "product.id-invalid", "业务 ID 必须大于0");
    }

    private void validateVersion(Long version) {
        Assert.isTrue(version == null || version < 0, "product.version-invalid", "期望版本不能为空且不能小于0");
    }

    private void validateStatus(String status) {
        ProductEnum.Status value = ProductEnum.Status.ofCode(status);
        Assert.isNull(value, "product.status-invalid", "状态必须为 ENABLED 或 DISABLED");
    }

    private void validateText(String text, int maximumLength, boolean required) {
        Assert.isTrue(required && (text == null || text.isBlank()), "product.text-required", "必填文本不能为空");
        boolean tooLong = text != null && text.trim().length() > maximumLength;
        Assert.isTrue(tooLong, "product.text-too-long", "文本长度不能超过{0}个字符", maximumLength);
    }

    private void validateNumber(BigDecimal number, boolean required, boolean nonnegative) {
        Assert.isTrue(required && number == null, "product.number-required", "数量不能为空");
        if (number == null) {
            return;
        }
        BigDecimal normalized = number.stripTrailingZeros();
        BigDecimal upperExclusive = new BigDecimal("100000000000000");
        boolean invalid = normalized.scale() > 6 || number.abs().compareTo(upperExclusive) >= 0;
        Assert.isTrue(invalid, "product.number-precision", "数值最多14位整数和6位小数");
        Assert.isTrue(nonnegative && number.signum() < 0, "product.number-negative", "数量及价格不能为负数");
    }
}
