package com.muzhi.minierp.controller.sales;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.controller.product.ProductQueryValidation;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.muzhi.minierp.service.sales.IQuotationService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import com.muzhi.minierp.vo.product.ProductQuotationQueryVO;
import com.muzhi.minierp.vo.sales.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 报价选品、计价预览及正式报价接口。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@RestController
@RequestMapping("/quotation")
@RequiredArgsConstructor
public class QuotationController {

    private final IQuotationService quotationService;

    private final IProductSkuService productSkuService;

    @GetMapping("/sku/options")
    @PreAuthorize("hasAnyAuthority('sales-manager:quotation:create', 'sales-manager:quotation:update')")
    public JsonResult<IPage<ProductOptionVO>> options(ProductQuotationQueryVO query, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "15") Integer pageSize) {
        QuotationController.validateOptions(query, pageNum, pageSize);
        IPage<ProductOptionVO> result = productSkuService.quotationOptions(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('sales-manager:quotation:create')")
    public JsonResult<QuotationDocumentVO> preview(@RequestBody QuotationFormVO form) {
        this.validateForm(form, false);
        QuotationDocumentVO result = quotationService.preview(form);
        return JsonResult.success(result);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales-manager:quotation:create')")
    public JsonResult<QuotationSummaryVO> create(@RequestBody QuotationFormVO form) {
        this.validateForm(form, false);
        QuotationSummaryVO result = quotationService.create(form);
        return JsonResult.success(result);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sales-manager:quotation:query')")
    public JsonResult<IPage<QuotationSummaryVO>> list(
            QuotationQueryVO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize
    ) {
        QuotationController.validatePage(pageNum, pageSize);
        QuotationController.validateText(query.getKeyword(), 256, 0);
        QuotationController.validateText(query.getQuoteNumber(), 16, 0);
        QuotationController.validateText(query.getTitle(), 256, 0);
        QuotationController.validateText(query.getCustomerName(), 256, 0);
        LocalDate from = query.getQuotedFrom();
        LocalDate to = query.getQuotedTo();
        boolean invalid = (from != null && to != null && from.isAfter(to)) || LocalDate.MAX.equals(to);
        Assert.isTrue(invalid, "quotation.date-range-invalid", "报价日期范围无效");
        IPage<QuotationSummaryVO> result = quotationService.list(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('sales-manager:quotation:query')")
    public JsonResult<QuotationDetailVO> detail(@RequestParam Long id) {
        QuotationController.validateId(id);
        QuotationDetailVO result = quotationService.detail(id);
        return JsonResult.success(result);
    }

    @PostMapping("/update/preview")
    @PreAuthorize("hasAuthority('sales-manager:quotation:update')")
    public JsonResult<QuotationDocumentVO> previewUpdate(@RequestBody QuotationFormVO form) {
        this.validateForm(form, true);
        QuotationDocumentVO result = quotationService.previewUpdate(form);
        return JsonResult.success(result);
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('sales-manager:quotation:update')")
    public JsonResult<QuotationDetailVO> update(@RequestBody QuotationFormVO form) {
        this.validateForm(form, true);
        QuotationDetailVO result = quotationService.update(form);
        return JsonResult.success(result);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('sales-manager:quotation:delete')")
    public JsonResult<Void> delete(@RequestBody QuotationVersionedId reference) {
        QuotationController.validateVersionedId(reference);
        quotationService.delete(reference);
        return JsonResult.success();
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('sales-manager:quotation:query') and hasAuthority('sales-manager:quotation:create')")
    public JsonResult<QuotationDetailVO> copy(@RequestBody QuotationCopyRequest request) {
        Assert.isNull(request, "quotation.copy-choice-required", "请选择是否清空客户资料");
        Assert.isNull(request.clearCustomer(), "quotation.copy-choice-required", "请选择是否清空客户资料");
        QuotationController.validateId(request.id());
        QuotationDetailVO result = quotationService.copy(request);
        return JsonResult.success(result);
    }

    static void validateId(Long id) {
        Assert.isTrue(id == null || id <= 0, "quotation.id-invalid", "业务 ID 必须大于 0");
    }

    static void validateVersionedId(QuotationVersionedId reference) {
        Assert.isNull(reference, "quotation.reference-required", "请提交业务 ID 和期望版本");
        QuotationController.validateId(reference.id());
        QuotationController.validateVersion(reference.expectedVersion());
    }

    static void validateVersion(Long version) {
        Assert.isTrue(version == null || version < 0, "quotation.version-invalid", "期望版本不能为空且不能小于 0");
    }

    static void validatePage(Integer pageNum, Integer pageSize) {
        Assert.isTrue(pageNum == null || pageNum <= 0, "pagination.page-num-invalid", "当前页必须大于0");
        Assert.isTrue(pageSize == null || pageSize <= 0, "pagination.page-size-invalid", "每页数量必须大于0");
    }

    static void validateOptions(ProductQuotationQueryVO query, Integer pageNum, Integer pageSize) {
        ProductQueryValidation.validate(query, pageNum, pageSize);
        String currency = query.getReferenceSaleCurrency();
        boolean invalid = currency != null && CurrencyEnum.ofCode(currency) == null;
        Assert.isTrue(invalid, "quotation.currency-invalid", "报价币种必须为 CNY 或 TWD");
        if (query.getSkuId() != null) {
            QuotationController.validateId(query.getSkuId());
        }
    }

    /**
     * 检查报价行来源、数量存储精度和两级优惠字段。
     */
    private void validateForm(QuotationFormVO form, boolean editing) {
        Assert.isNull(form, "quotation.form-required", "请提交报价表单");
        if (editing || form.reference() != null) {
            QuotationController.validateVersionedId(form.reference());
        }
        if (editing) {
            Assert.isNull(form.quotedAt(), "quotation.quoted-at-required", "报价时间不能为空");
        }
        QuotationController.validateTitle(form.title(), editing);
        this.validateCustomer(form.customer());
        QuotationController.validateText(form.remark(), 1024, 0);
        this.validateDiscount(form.discount(), 0);
        List<QuotationFormVO.Item> items = form.items();
        Assert.isEmpty(items, "quotation.items-required", "报价至少需要一行商品");
        Set<Long> retainedIds = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            int position = index + 1;
            QuotationFormVO.Item item = items.get(index);
            Assert.isNull(item, "quotation.line-invalid", "第 {0} 行的选品信息无效", position);
            if (item.sourceItemId() != null) {
                boolean invalid = form.reference() == null || item.sourceItemId() <= 0 || item.skuId() != null
                        || item.productVersion() != null || item.skuVersion() != null;
                Assert.isTrue(invalid, "quotation.line-invalid", "第 {0} 行的选品信息无效", position);
                Assert.isTrue(editing && !retainedIds.add(item.sourceItemId()), "quotation.line-source-duplicate",
                        "第 {0} 行重复使用了同一原报价明细", position);
            } else {
                boolean invalid = item.skuId() == null || item.skuId() <= 0
                        || item.productVersion() == null || item.productVersion() < 0
                        || item.skuVersion() == null || item.skuVersion() < 0;
                Assert.isTrue(invalid, "quotation.line-invalid", "第 {0} 行的选品信息无效", position);
            }
            this.validateQuantity(item.quantity(), position);
            this.validateDiscount(item.discount(), position);
            QuotationController.validateText(item.remark(), 1024, position);
        }
    }

    static void validateTitle(String title, boolean required) {
        boolean invalid = title == null ? required : title.isBlank();
        Assert.isTrue(invalid, "quotation.title-required", "报价标题不能为空");
        if (title != null) {
            QuotationController.validateText(title.strip(), 256, 0);
        }
    }

    private void validateCustomer(QuotationCustomerVO customer) {
        if (customer != null) {
            QuotationController.validateText(customer.getCustomerName(), 256, 0);
            QuotationController.validateText(customer.getContactName(), 64, 0);
            QuotationController.validateText(customer.getContactPhone(), 32, 0);
            QuotationController.validateText(customer.getShippingAddress(), 512, 0);
        }
    }

    static void validateText(String text, int maximumLength, int position) {
        if (text == null) {
            return;
        }
        boolean invalid = text.codePointCount(0, text.length()) > maximumLength || text.indexOf(0) >= 0;
        if (position == 0) {
            Assert.isTrue(invalid, "quotation.text-invalid", "文本不能超过 {0} 字符或包含空字符", maximumLength);
        } else {
            Assert.isTrue(invalid, "quotation.line-text-invalid", "第 {0} 行的备注不能超过 {1} 字符或包含空字符",
                    position, maximumLength);
        }
    }

    private void validateQuantity(BigDecimal quantity, int position) {
        BigDecimal upperExclusive = new BigDecimal("100000000000000");
        boolean invalid = quantity == null || quantity.signum() <= 0
                || quantity.compareTo(upperExclusive) >= 0 || quantity.stripTrailingZeros().scale() > 6;
        Assert.isTrue(invalid, "quotation.quantity-invalid", "第 {0} 行的数量必须大于 0，最多 14 位整数和 6 位小数", position);
    }

    private void validateDiscount(QuotationFormVO.Discount discount, int position) {
        if (discount == null) {
            return;
        }
        BigDecimal rate = discount.discountRate();
        BigDecimal amount = discount.discountedAmount();
        boolean conflicting = rate != null && amount != null;
        BigDecimal upperExclusive = new BigDecimal("100000000000000");
        boolean invalidAmount = amount != null && (amount.signum() < 0
                || amount.compareTo(upperExclusive) >= 0 || amount.scale() > 2);
        QuotationController.checkDiscount(conflicting || invalidAmount, position);
        QuotationController.validateDiscountRate(rate, position);
    }

    static void validateDiscountRate(BigDecimal rate, int position) {
        boolean invalid = rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0
                || rate.scale() > 2);
        QuotationController.checkDiscount(invalid, position);
    }

    private static void checkDiscount(boolean invalid, int position) {
        if (position == 0) {
            Assert.isTrue(invalid, "quotation.discount-invalid",
                    "请选择一种整单优惠方式。折扣比例为 0 至 100，优惠后金额不能为负，两者最多两位小数");
        } else {
            Assert.isTrue(invalid, "quotation.line-discount-invalid",
                    "第 {0} 行的优惠有误。请选择一种优惠方式，折扣比例为 0 至 100，优惠后金额不能为负，两者最多两位小数",
                    position);
        }
    }
}
