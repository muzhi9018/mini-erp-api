package com.muzhi.minierp.controller.website;

import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import com.muzhi.minierp.entity.website.WebsiteProductDetailItem;
import com.muzhi.minierp.entity.website.WebsiteProductMediaItem;
import com.muzhi.minierp.enums.SysLocale;
import com.muzhi.minierp.enums.WebsiteProductEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.website.IWebsiteProductService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.website.WebsiteProductCreatedVO;
import com.muzhi.minierp.vo.website.WebsiteProductVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * 官网商品 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@RestController
@RequestMapping("/websiteProduct")
@RequiredArgsConstructor
public class WebsiteProductController {

    private final IWebsiteProductService websiteProductService;


    @PostMapping
    public JsonResult<WebsiteProductCreatedVO> create(@RequestBody WebsiteProductVO query) {
        Assert.isNull(query, "商品信息不能为空");
        Assert.isTrue(StringUtils.isBlank(query.getSlug()), "商品 slug 不能为空");
        String slug = query.getSlug().trim();
        Pattern compile = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
        Assert.isFalse(compile.matcher(slug).matches(), "商品 slug 只能由小写字母、数字和中划线组成");
        WebsiteProductI18nVO productI18n = query.getProductI18n();
        this.validateLocale(productI18n.getLocale());
        this.validateI18n(query.getProductI18n());
        WebsiteProductCreatedVO product = websiteProductService.create(query);
        return JsonResult.success(product);
    }

    /** 为已有商品新增一种语言，不覆盖已有语言。 */
    @PostMapping("/addI18n")
    public JsonResult<WebsiteProductCreatedVO> addI18n(@RequestBody WebsiteProductI18nVO query) {
        this.validateI18n(query);
        this.validateLocale(query.getLocale());
        WebsiteProductCreatedVO product = websiteProductService.addI18n(query);
        return JsonResult.success(product);
    }


    private void validateLocale(String code) {
        Assert.isTrue(StringUtils.isBlank(code), "语言编码不能为空");
        SysLocale locale = SysLocale.ofCode(code);
        Assert.isNull(locale, "语言编码格式不正确");
    }
    /**
     * 校验国际化数据
     * @author Mr.Muzhi
     * @since 2026/9/10 18:12
     * @param query query
     */
    private void validateI18n(WebsiteProductI18nVO query) {
        Assert.isNull(query, "商品语言内容不能为空");
        Assert.isTrue(StringUtils.isBlank(query.getName()), "商品名称不能为空");

        List<WebsiteProductDetailItem> features = query.getFeatures();
        this.validateDetailItem(features, WebsiteProductEnum.DetailItemType.FEATURE);

        List<WebsiteProductDetailItem> specifications = query.getSpecifications();
        this.validateDetailItem(specifications, WebsiteProductEnum.DetailItemType.SPECIFICATION);

        List<WebsiteProductMediaItem> applications = query.getApplications();
        this.validateMediaItem(applications, WebsiteProductEnum.MediaItemType.APPLICATION);
        List<WebsiteProductMediaItem> cases = query.getCases();
        this.validateMediaItem(cases, WebsiteProductEnum.MediaItemType.CASE);
    }

    /**
     * 校验 产品特点与优势 和 产品技术参数
     * @author Mr.Muzhi
     * @since 2026/9/10 18:12
     * @param detailItems 数据列表
     * @param detailItemType 类型
     */
    private void validateDetailItem(List<WebsiteProductDetailItem> detailItems, WebsiteProductEnum.DetailItemType detailItemType) {
        String name = detailItemType == WebsiteProductEnum.DetailItemType.FEATURE ? "产品特点与优势" : "产品技术参数";
        Assert.isEmpty(detailItems, name + "不能为空");
        Assert.isTrue(detailItems.size() < 4, "最少填写4个" + name);
        int index = 0;
        for (WebsiteProductDetailItem detailItem : detailItems) {
            Assert.isTrue(StringUtils.isBlank(detailItem.getTitle()), name + "标题不能为空");
            Assert.isTrue(StringUtils.isBlank(detailItem.getContent()), name + "内容不能为空");
            detailItem.setSortOrder(index++);
        }
    }

    /**
     * 校验 产品应用场景 和 产品案例展示
     * @author Mr.Muzhi
     * @since 2026/9/10 18:13
     * @param mediaItems 数据列表
     * @param mediaItemType 类型
     */
    private void validateMediaItem(List<WebsiteProductMediaItem> mediaItems, WebsiteProductEnum.MediaItemType mediaItemType) {
        String name = mediaItemType == WebsiteProductEnum.MediaItemType.APPLICATION ? "产品应用场景" : "产品案例展示";
        Assert.isEmpty(mediaItems, name + "不能为空");
        Assert.isTrue(mediaItems.size() < 4, "最少填写4个" + name);
        int index = 0;
        for (WebsiteProductMediaItem mediaItem : mediaItems) {
            Assert.isTrue(StringUtils.isBlank(mediaItem.getTitle()), name + "标题不能为空");
            Assert.isTrue(StringUtils.isBlank(mediaItem.getImageUrl()), name + "图片地址不能为空");
            mediaItem.setSortOrder(index++);
        }
    }
}
