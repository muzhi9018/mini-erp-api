package com.muzhi.minierp.controller.sales;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.enums.QuotationEnum.Scope;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.muzhi.minierp.service.sales.IQuotationTemplateService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import com.muzhi.minierp.vo.product.ProductQuotationQueryVO;
import com.muzhi.minierp.vo.sales.QuotationTemplateVO;
import com.muzhi.minierp.vo.sales.QuotationVO;
import com.muzhi.minierp.vo.sales.QuotationTemplateItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个人和公共报价模板的独立路由。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@RestController
@RequestMapping("/quotation/template")
@RequiredArgsConstructor
public class QuotationTemplateController {

    private final IQuotationTemplateService templateService;

    private final IProductSkuService productSkuService;

    @GetMapping("/personal/list")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:query')")
    public JsonResult<IPage<QuotationTemplateVO>> personalList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize
    ) {
        return this.list(Scope.PERSONAL, keyword, pageNum, pageSize);
    }

    @GetMapping("/personal/detail")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:query')")
    public JsonResult<QuotationTemplateVO> personalDetail(@RequestParam Long id) {
        return this.detail(Scope.PERSONAL, id);
    }

    @PostMapping("/personal/create")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:create')")
    public JsonResult<QuotationTemplateVO> personalCreate(@RequestBody QuotationTemplateVO form) {
        this.validateForm(form, false);
        QuotationTemplateVO result = templateService.create(Scope.PERSONAL, form);
        return JsonResult.success(result);
    }

    @PostMapping("/personal/update")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:update')")
    public JsonResult<QuotationTemplateVO> personalUpdate(@RequestBody QuotationTemplateVO form) {
        this.validateForm(form, true);
        QuotationTemplateVO result = templateService.update(Scope.PERSONAL, form);
        return JsonResult.success(result);
    }

    @PostMapping("/personal/delete")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:delete')")
    public JsonResult<Void> personalDelete(@RequestBody QuotationTemplateVO reference) {
        Assert.isNull(reference, "quotation.reference-required", "请提交业务 ID 和期望版本");
        QuotationController.validateVersionedId(reference.getId(), reference.getVersion());
        templateService.delete(Scope.PERSONAL, reference);
        return JsonResult.success();
    }

    @PostMapping("/personal/apply")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:personal:query') and hasAuthority('sales-manager:quotation:create')")
    public JsonResult<QuotationVO> personalApply(@RequestBody QuotationTemplateVO request) {
        return this.apply(Scope.PERSONAL, request);
    }

    @GetMapping("/personal/sku/options")
    @PreAuthorize("hasAnyAuthority('sales-manager:quotation-template:personal:create', 'sales-manager:quotation-template:personal:update')")
    public JsonResult<IPage<ProductOptionVO>> personalOptions(ProductQuotationQueryVO query, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "15") Integer pageSize) {
        QuotationController.validateOptions(query, pageNum, pageSize);
        IPage<ProductOptionVO> result = productSkuService.quotationOptions(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    @GetMapping("/public/list")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:public:query')")
    public JsonResult<IPage<QuotationTemplateVO>> publicList(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "15") Integer pageSize) {
        return this.list(Scope.PUBLIC, keyword, pageNum, pageSize);
    }

    @GetMapping("/public/detail")
    public JsonResult<QuotationTemplateVO> publicDetail(@RequestParam Long id) {
        return this.detail(Scope.PUBLIC, id);
    }

    @PostMapping("/public/create")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:public:create')")
    public JsonResult<QuotationTemplateVO> publicCreate(@RequestBody QuotationTemplateVO form) {
        this.validateForm(form, false);
        QuotationTemplateVO result = templateService.create(Scope.PUBLIC, form);
        return JsonResult.success(result);
    }

    @PostMapping("/public/update")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:public:update')")
    public JsonResult<QuotationTemplateVO> publicUpdate(@RequestBody QuotationTemplateVO form) {
        this.validateForm(form, true);
        QuotationTemplateVO result = templateService.update(Scope.PUBLIC, form);
        return JsonResult.success(result);
    }

    @PostMapping("/public/delete")
    @PreAuthorize("hasAuthority('sales-manager:quotation-template:public:delete')")
    public JsonResult<Void> publicDelete(@RequestBody QuotationTemplateVO reference) {
        Assert.isNull(reference, "quotation.reference-required", "请提交业务 ID 和期望版本");
        QuotationController.validateVersionedId(reference.getId(), reference.getVersion());
        templateService.delete(Scope.PUBLIC, reference);
        return JsonResult.success();
    }

    @PostMapping("/public/apply")
    @PreAuthorize("hasAuthority('sales-manager:quotation:create')")
    public JsonResult<QuotationVO> publicApply(@RequestBody QuotationTemplateVO request) {
        return this.apply(Scope.PUBLIC, request);
    }

    @GetMapping("/public/sku/options")
    @PreAuthorize("hasAnyAuthority('sales-manager:quotation-template:public:create', 'sales-manager:quotation-template:public:update')")
    public JsonResult<IPage<ProductOptionVO>> publicOptions(
            ProductQuotationQueryVO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize
    ) {
        QuotationController.validateOptions(query, pageNum, pageSize);
        IPage<ProductOptionVO> result = productSkuService.quotationOptions(query, pageNum, pageSize);
        return JsonResult.success(result);
    }

    private JsonResult<IPage<QuotationTemplateVO>> list(Scope scope, String keyword, Integer pageNum, Integer pageSize) {
        QuotationController.validatePage(pageNum, pageSize);
        QuotationController.validateText(keyword, 256, 0);
        IPage<QuotationTemplateVO> result = templateService.list(scope, keyword, pageNum, pageSize);
        return JsonResult.success(result);
    }

    private JsonResult<QuotationTemplateVO> detail(Scope scope, Long id) {
        QuotationController.validateId(id);
        QuotationTemplateVO result = templateService.detail(scope, id);
        return JsonResult.success(result);
    }

    private JsonResult<QuotationVO> apply(Scope scope, QuotationTemplateVO request) {
        Assert.isNull(request, "quotation.id-invalid", "业务 ID 必须大于 0");
        QuotationController.validateId(request.getId());
        QuotationVO result = templateService.apply(scope, request.getId());
        return JsonResult.success(result);
    }

    /**
     * 校验模板的完整表单，报价标题沿用报价表单规则。
     */
    private void validateForm(QuotationTemplateVO form, boolean editing) {
        Assert.isNull(form, "quotation.template.form-required", "请提交报价模板表单");
        if (editing) {
            QuotationController.validateId(form.getId());
            QuotationController.validateVersion(form.getVersion());
        }
        Assert.isTrue(form.getName() == null || form.getName().isBlank(), "quotation.template.name-required", "模板名称不能为空");
        QuotationController.validateText(form.getName().strip(), 128, 0);
        QuotationController.validateTitle(form.getTitle(), true);
        QuotationController.validateText(form.getRemark(), 1024, 0);
        QuotationController.validateDiscountRate(form.getDiscountRate(), 0);
        List<QuotationTemplateItemVO> items = form.getItems();
        Assert.isEmpty(items, "quotation.items-required", "报价至少需要一行商品");
        for (int index = 0; index < items.size(); index++) {
            int position = index + 1;
            QuotationTemplateItemVO item = items.get(index);
            boolean invalid = item == null || item.getSkuId() == null || item.getSkuId() <= 0;
            Assert.isTrue(invalid, "quotation.line-invalid", "第 {0} 行的选品信息无效", position);
            QuotationController.validateDiscountRate(item.getDiscountRate(), position);
            QuotationController.validateText(item.getRemark(), 1024, position);
        }
    }
}
