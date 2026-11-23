package com.muzhi.minierp.controller.product;

import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.product.ProductQueryVO;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品管理和单据选品共用的 SKU 筛选参数校验。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public final class ProductQueryValidation {

    private ProductQueryValidation() {
    }

    /**
     * 校验分页和结构化规格筛选条件。
     */
    public static void validate(ProductQueryVO query, Integer pageNum, Integer pageSize) {
        Assert.isTrue(pageNum == null || pageNum <= 0, "pagination.page-num-invalid", "当前页必须大于0");
        Assert.isTrue(pageSize == null || pageSize <= 0, "pagination.page-size-invalid", "每页数量必须大于0");
        if (query.getCategoryId() != null) {
            ProductQueryValidation.validateId(query.getCategoryId());
        }
        if (query.getProductId() != null) {
            ProductQueryValidation.validateId(query.getProductId());
        }
        if (query.getStatus() != null) {
            ProductQueryValidation.validateStatus(query.getStatus());
        }
        if (query.getSkuStatus() != null) {
            ProductQueryValidation.validateStatus(query.getSkuStatus());
        }
        if (query.getSpecs() == null) {
            query.setSpecs(List.of());
        }
        Set<Long> specIds = new HashSet<>();
        for (ProductQueryVO.SpecFilter filter : query.getSpecs()) {
            Assert.isNull(filter, "product.spec.filter-required", "规格筛选不能为空");
            ProductQueryValidation.validateId(filter.getSpecId());
            Assert.isTrue(!specIds.add(filter.getSpecId()), "product.spec.filter-duplicate", "同一维度不能重复筛选");
            boolean textFilter = filter.getSpecValueId() != null;
            boolean rangeFilter = filter.getMinimum() != null || filter.getMaximum() != null;
            Assert.isTrue(textFilter == rangeFilter, "product.spec.filter-invalid", "请选择候选值或数值区间其中一种筛选方式");
            if (textFilter) {
                ProductQueryValidation.validateId(filter.getSpecValueId());
            }
            ProductQueryValidation.validateNumber(filter.getMinimum(), false, false);
            ProductQueryValidation.validateNumber(filter.getMaximum(), false, false);
            boolean reversed = filter.getMinimum() != null && filter.getMaximum() != null && filter.getMinimum().compareTo(filter.getMaximum()) > 0;
            Assert.isTrue(reversed, "product.spec.range-invalid", "规格区间下限不能大于上限");
        }
    }

    private static void validateId(Long id) {
        Assert.isTrue(id == null || id <= 0, "product.id-invalid", "业务 ID 必须大于0");
    }

    private static void validateStatus(String status) {
        ProductEnum.Status value = ProductEnum.Status.ofCode(status);
        Assert.isNull(value, "product.status-invalid", "状态必须为 ENABLED 或 DISABLED");
    }

    static void validateNumber(BigDecimal number, boolean required, boolean nonnegative) {
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
