package com.muzhi.minierp.service.sales.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.product.Product;
import com.muzhi.minierp.entity.product.ProductSku;
import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.mapper.product.ProductMapper;
import com.muzhi.minierp.mapper.product.ProductSkuMapper;
import com.muzhi.minierp.mapper.sales.QuotationItemMapper;
import com.muzhi.minierp.model.sales.QuotationSkuSelection;
import com.muzhi.minierp.service.product.IProductImageService;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.muzhi.minierp.service.sales.IQuotationItemService;
import com.muzhi.minierp.vo.product.ProductImageVO;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 正式报价商品明细，使用商品快照和保存时的成交数据 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
@RequiredArgsConstructor
public class QuotationItemServiceImpl extends ServiceImpl<QuotationItemMapper, QuotationItem> implements IQuotationItemService {

    private final ProductMapper productMapper;

    private final ProductSkuMapper productSkuMapper;

    private final IProductSkuService productSkuService;

    private final IProductImageService productImageService;

    @Override
    public Map<Long, QuotationSkuSelection> loadSkuSelections(List<Long> skuIds, boolean forWrite) {
        List<Long> ids = skuIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<ProductSku> skus = productSkuMapper.selectByIds(ids);
        List<Long> productIds = skus.stream().map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productsById = new HashMap<>();
        if (!productIds.isEmpty()) {
            LambdaQueryWrapper<Product> productQuery = Wrappers.lambdaQuery();
            productQuery.in(Product::getId, productIds);
            productQuery.orderByAsc(Product::getId);
            productQuery.last(forWrite, "FOR UPDATE");
            List<Product> products = productMapper.selectList(productQuery);
            productsById = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        }
        if (forWrite && !skus.isEmpty()) {
            LambdaQueryWrapper<ProductSku> skuQuery = Wrappers.lambdaQuery();
            skuQuery.in(ProductSku::getId, ids);
            skuQuery.orderByAsc(ProductSku::getProductId, ProductSku::getId);
            skuQuery.last("FOR UPDATE");
            skus = productSkuMapper.selectList(skuQuery);
        }

        List<ProductOptionVO> options = productSkuService.optionsByIds(ids);
        Map<Long, List<ProductImageVO>> imagesByProduct = new HashMap<>();
        for (ProductOptionVO option : options) {
            if (option.getImages().isEmpty()) {
                List<ProductImageVO> images = imagesByProduct.computeIfAbsent(option.getProductId(), productImageService::listByProduct);
                option.setImages(images);
            }
        }
        Map<Long, ProductOptionVO> optionsById = options.stream()
                .collect(Collectors.toMap(ProductOptionVO::getSkuId, Function.identity()));
        Map<Long, QuotationSkuSelection> result = new HashMap<>();
        for (ProductSku sku : skus) {
            Product product = productsById.get(sku.getProductId());
            ProductOptionVO option = optionsById.get(sku.getId());
            List<String> issues = this.selectionIssues(sku, product, option);
            result.put(sku.getId(), new QuotationSkuSelection(sku.getReferenceSaleCurrency(), option, issues));
        }
        return result;
    }

    private List<String> selectionIssues(ProductSku sku, Product product, ProductOptionVO option) {
        List<String> issues = new ArrayList<>();
        String enabled = ProductEnum.Status.ENABLED.getCode();
        if (product == null) {
            issues.add("PRODUCT_MISSING");
        } else if (!enabled.equals(product.getStatus())) {
            issues.add("PRODUCT_DISABLED");
        }
        if (!enabled.equals(sku.getStatus())) {
            issues.add("SKU_DISABLED");
        }
        if (sku.getReferenceSalePrice() == null || sku.getReferenceSalePrice().signum() < 0) {
            issues.add("PRICE_MISSING");
        }
        CurrencyEnum currency = CurrencyEnum.ofCode(sku.getReferenceSaleCurrency());
        if (currency == null) {
            issues.add("CURRENCY_INVALID");
        }
        if (product != null && option == null) {
            issues.add("OPTION_UNAVAILABLE");
        }
        return issues;
    }
}
