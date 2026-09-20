package com.muzhi.minierp.controller.website;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import com.muzhi.minierp.enums.SysLocale;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.website.IWebsiteProductCategoryService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryI18nVO;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * {@code WebsiteProductCategoryController}: 官网商品分类 Controller
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/11 20:41
 */
@RestController
@RequestMapping("/website/product/category")
@RequiredArgsConstructor
public class WebsiteProductCategoryController {

    private final IWebsiteProductCategoryService websiteProductCategoryService;

    /**
     * 新增商品分类及默认语言内容。
     */
    @PostMapping("/create")
    public JsonResult<WebsiteProductCategoryI18n> create(@RequestBody WebsiteProductCategoryVO query) {
        Assert.isNull(query, "website.product-category.required", "商品分类信息不能为空");
        boolean codeBlank = StringUtils.isBlank(query.getCode());
        Assert.isTrue(codeBlank, "website.product-category.code-required", "商品分类编码不能为空");
        SysLocale locale = this.validateI18n(query.getCategoryI18n());
        Assert.isFalse(locale.isDefaultLocal(), "website.locale.default-required", "新增时只能使用系统默认语言");
        WebsiteProductCategoryI18n category = websiteProductCategoryService.create(query);
        return JsonResult.success(category);
    }

    /**
     * 为已有商品分类添加一种语言，不覆盖已有语言。
     */
    @PostMapping("/addI18n")
    public JsonResult<WebsiteProductCategoryI18n> addI18n(@RequestBody WebsiteProductCategoryI18n query) {
        this.validateI18n(query);
        Assert.isNull(query.getWebsiteProductCategoryId(), "website.product-category.id-required", "商品分类 ID 不能为空");

        WebsiteProductCategoryI18n category = websiteProductCategoryService.addI18n(query);
        return JsonResult.success(category);
    }

    /**
     * 校验分类语言内容及系统支持的语言编码。
     */
    private SysLocale validateI18n(WebsiteProductCategoryI18n query) {
        Assert.isNull(query, "website.product-category.translation-required", "商品分类语言内容不能为空");
        boolean nameBlank = StringUtils.isBlank(query.getName());
        Assert.isTrue(nameBlank, "website.product-category.name-required", "商品分类名称不能为空");

        String localeCode = query.getLocale();
        boolean localeCodeBlank = StringUtils.isBlank(localeCode);
        Assert.isTrue(localeCodeBlank, "website.locale.code-required", "语言编码不能为空");
        SysLocale locale = SysLocale.ofCode(localeCode);
        Assert.isNull(locale, "website.locale.code-invalid", "语言编码格式不正确");
        return locale;
    }

    @GetMapping("/list")
    public JsonResult<IPage<WebsiteProductCategoryI18nVO>> list(WebsiteProductCategoryI18nVO query, @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam("pageSize") Integer pageSize) {
        IPage<WebsiteProductCategoryI18nVO> list = websiteProductCategoryService.list(query, pageNum, pageSize);
        return  JsonResult.success(list);
    }
}
