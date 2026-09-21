package com.muzhi.minierp.service.website.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.enums.SysLocale;
import com.muzhi.minierp.enums.WebsiteProductEnum;
import com.muzhi.minierp.i18n.I18nContext;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import com.muzhi.minierp.entity.website.*;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.website.WebsiteProductCategoryMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductDetailItemMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductI18nMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductMediaItemMapper;
import com.muzhi.minierp.service.website.IWebsiteProductService;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.website.WebsiteProductCreatedVO;
import com.muzhi.minierp.vo.website.WebsiteProductMediaItemVO;
import com.muzhi.minierp.vo.website.WebsiteProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 官网商品新增与语言内容新增。 */
@Service
@RequiredArgsConstructor
public class WebsiteProductServiceImpl extends ServiceImpl<WebsiteProductMapper, WebsiteProduct> implements IWebsiteProductService {

    private final IAttachmentService attachmentService;

    private final WebsiteProductCategoryMapper websiteProductCategoryMapper;

    private final WebsiteProductI18nMapper websiteProductI18nMapper;

    private final WebsiteProductDetailItemMapper websiteProductDetailItemMapper;

    private final WebsiteProductMediaItemMapper websiteProductMediaItemMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCreatedVO create(WebsiteProductVO query) {
        WebsiteProductCategory dbCategory = websiteProductCategoryMapper.selectById(query.getCategoryId());
        Assert.isNull(dbCategory, "website.product.category-not-found", "商品分类不存在");
        String slug = query.getSlug();
        LambdaQueryWrapper<WebsiteProduct> slugQuery = Wrappers.lambdaQuery();
        slugQuery.eq(WebsiteProduct::getSlug, slug);
        Assert.isTrue(baseMapper.exists(slugQuery), "website.product.slug-already-exists", "商品 slug 已存在");

        WebsiteProductI18nVO productI18nDTO = query.getProductI18n();
        String locale = productI18nDTO.getLocale();

        WebsiteProduct product = new WebsiteProduct();
        product.setId(IdWorker.getId());
        product.setCategoryId(query.getCategoryId());
        product.setSlug(slug);
        Integer sortOrder = query.getSortOrder();
        product.setSortOrder(sortOrder == null ? 0 : sortOrder);
        product.setIsShow(false);
        product.setIsRecommended(Boolean.TRUE.equals(query.getIsRecommended()));
        product.setIsShow(true);
        product.setIsDeleted(false);
        try {
            baseMapper.insert(product);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("website.product.slug-already-exists", "商品 slug 已存在", exception);
        }

        return this.insertI18n(product.getId(), locale, query.getProductI18n());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCreatedVO addI18n(WebsiteProductI18nVO query) {
        Long productId = query.getProductId();
        String locale = query.getLocale();
        // 在事务内锁住父商品，避免两个添加语言请求同时通过重复检查。
        WebsiteProduct dbProduct = baseMapper.selectByIdForUpdate(productId);
        Assert.isNull(dbProduct, "website.product.not-found", "商品不存在");
        LambdaQueryWrapper<WebsiteProductI18n> productI18nQuery = Wrappers.lambdaQuery();
        productI18nQuery.eq(WebsiteProductI18n::getProductId, productId);
        productI18nQuery.eq(WebsiteProductI18n::getLocale, locale);
        Assert.isTrue(websiteProductI18nMapper.exists(productI18nQuery), "website.product.locale-already-exists", "该商品已配置此语言");
        return this.insertI18n(productId, locale, query);
    }

    private WebsiteProductCreatedVO insertI18n(Long productId, String locale, WebsiteProductI18nVO query) {
        List<Long> attachmentIds = new ArrayList<>();
        attachmentIds.add(query.getCoverImageAttachmentId());
        attachmentIds.add(query.getFeatureImageAttachmentId());
        for (WebsiteProductMediaItem item : query.getApplications()) {
            attachmentIds.add(item.getImageAttachmentId());
        }
        for (WebsiteProductMediaItem item : query.getCases()) {
            attachmentIds.add(item.getImageAttachmentId());
        }
        attachmentService.validateAttachments(attachmentIds);
        WebsiteProductI18n translation = BeanConvertUtils.convert(query, WebsiteProductI18n.class);
        translation.setGmtCreate(null);
        translation.setGmtModified(null);
        translation.setCreateUser(null);
        translation.setUpdateUser(null);
        translation.setId(IdWorker.getId());
        translation.setProductId(productId);
        translation.setLocale(locale);
        translation.setName(query.getName().trim());
        translation.setIsDeleted(false);
        try {
            websiteProductI18nMapper.insert(translation);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("website.product.locale-already-exists", "该商品已配置此语言", exception);
        }

        List<WebsiteProductDetailItem> detailItems = new ArrayList<>(16);
        List<WebsiteProductDetailItem> features = query.getFeatures();
        for (WebsiteProductDetailItem feature : features) {
             this.setDefaultDetailItemParams(feature, translation.getId());
            detailItems.add(feature);
        }

        List<WebsiteProductDetailItem> specifications = query.getSpecifications();
        for (WebsiteProductDetailItem specification : specifications) {
            this.setDefaultDetailItemParams(specification, translation.getId());
            detailItems.add(specification);
        }

        websiteProductDetailItemMapper.insert(detailItems);

        List<WebsiteProductMediaItem> mediaItems = new ArrayList<>(16);
        List<WebsiteProductMediaItem> applications = BeanConvertUtils.mapList(query.getApplications(), WebsiteProductMediaItem.class);
        for (WebsiteProductMediaItem application : applications) {
            this.setDefaultMediaItemParams(application, translation.getId());
            mediaItems.add(application);
        }
        List<WebsiteProductMediaItem> cases = BeanConvertUtils.mapList(query.getCases(), WebsiteProductMediaItem.class);
        for (WebsiteProductMediaItem item : cases) {
             this.setDefaultMediaItemParams(item, translation.getId());
            mediaItems.add(item);
        }
        websiteProductMediaItemMapper.insert(mediaItems);
        return new WebsiteProductCreatedVO(productId, translation.getId(), locale);
    }


    /**
     * 设置默认 官网商品文本明细 参数
     * @author Mr.Muzhi
     * @since 2026/9/10 18:15
     * @param detailItem 官网商品文本明细
     * @param productI18nId 国际化id
     */
    private void setDefaultDetailItemParams(WebsiteProductDetailItem detailItem, Long productI18nId) {
        detailItem.setId(IdWorker.getId());
        detailItem.setProductI18nId(productI18nId);
        detailItem.setIsShow(!Boolean.FALSE.equals(detailItem.getIsShow()));
        detailItem.setIsDeleted(false);
    }

    /**
     * 设置默认 官网商品媒体明细 参数
     * @author Mr.Muzhi
     * @since 2026/9/10 18:15
     * @param mediaItem 官网商品媒体明细
     * @param productI18nId 国际化id
     */
    private void setDefaultMediaItemParams(WebsiteProductMediaItem mediaItem, Long productI18nId) {
        mediaItem.setId(IdWorker.getId());
        mediaItem.setProductI18nId(productI18nId);
        mediaItem.setIsShow(!Boolean.FALSE.equals(mediaItem.getIsShow()));
        mediaItem.setIsDeleted(false);
        mediaItem.setGmtCreate(null);
        mediaItem.setGmtModified(null);
        mediaItem.setCreateUser(null);
        mediaItem.setUpdateUser(null);
    }


    @Override
    public IPage<WebsiteProductI18nVO> list(WebsiteProductI18nVO query, Integer pageNum, Integer pageSize) {
        SysLocale currentLocale = I18nContext.getCurrentLocale();
        query.setLocale(currentLocale.getCode());
        Page<WebsiteProductI18nVO> page = new Page<>(pageNum, pageSize);
        IPage<WebsiteProductI18nVO> result = websiteProductI18nMapper.list(page, query);
        this.fillImageUrls(result.getRecords());
        return result;
    }

    /**
     * 批量加载当前页媒体和附件，避免逐条访问数据库；授权地址不持久化。
     */
    private void fillImageUrls(List<WebsiteProductI18nVO> products) {
        if (products.isEmpty()) {
            return;
        }
        List<Long> translationIds = products.stream().map(WebsiteProductI18nVO::getProductI18nId).toList();
        LambdaQueryWrapper<WebsiteProductMediaItem> mediaQuery = Wrappers.lambdaQuery();
        mediaQuery.in(WebsiteProductMediaItem::getProductI18nId, translationIds);
        mediaQuery.orderByAsc(WebsiteProductMediaItem::getSortOrder, WebsiteProductMediaItem::getId);
        List<WebsiteProductMediaItem> mediaItems = websiteProductMediaItemMapper.selectList(mediaQuery);
        List<WebsiteProductMediaItemVO> mediaViews = BeanConvertUtils.mapList(mediaItems, WebsiteProductMediaItemVO.class);
        List<Long> attachmentIds = new ArrayList<>();
        for (WebsiteProductI18nVO product : products) {
            attachmentIds.add(product.getCoverImageAttachmentId());
            attachmentIds.add(product.getFeatureImageAttachmentId());
        }
        for (WebsiteProductMediaItemVO media : mediaViews) {
            attachmentIds.add(media.getImageAttachmentId());
        }
        Map<Long, String> urls = attachmentService.getAuthorizedUrls(attachmentIds);
        for (WebsiteProductMediaItemVO media : mediaViews) {
            media.setImageUrl(media.getImageAttachmentId() == null ? null : urls.get(media.getImageAttachmentId()));
        }
        Map<Long, List<WebsiteProductMediaItemVO>> mediaByTranslation = mediaViews.stream()
                .collect(Collectors.groupingBy(WebsiteProductMediaItem::getProductI18nId));
        for (WebsiteProductI18nVO product : products) {
            product.setCoverImageUrl(product.getCoverImageAttachmentId() == null ? null : urls.get(product.getCoverImageAttachmentId()));
            product.setFeatureImageUrl(product.getFeatureImageAttachmentId() == null ? null : urls.get(product.getFeatureImageAttachmentId()));
            List<WebsiteProductMediaItemVO> translationMedia = mediaByTranslation.getOrDefault(product.getProductI18nId(), List.of());
            List<WebsiteProductMediaItemVO> applications = translationMedia.stream()
                    .filter(item -> WebsiteProductEnum.MediaItemType.APPLICATION.getCode().equals(item.getItemType()))
                    .toList();
            List<WebsiteProductMediaItemVO> cases = translationMedia.stream()
                    .filter(item -> WebsiteProductEnum.MediaItemType.CASE.getCode().equals(item.getItemType()))
                    .toList();
            product.setApplications(applications);
            product.setCases(cases);
        }
    }

    @Override
    public List<WebsiteProductI18nVO> websiteList(Long categoryId, Boolean recommended) {
        SysLocale currentLocale = I18nContext.getCurrentLocale();
        LambdaQueryWrapper<WebsiteProduct> query = Wrappers.lambdaQuery();
        if (categoryId != null) {
            query.eq(WebsiteProduct::getCategoryId, categoryId);
        }
        if (recommended != null) {
            query.eq(WebsiteProduct::getIsRecommended, recommended);
        }
        query.eq(WebsiteProduct::getIsShow, true);
        query.orderByAsc(WebsiteProduct::getSortOrder);
        query.orderByDesc(WebsiteProduct::getId);
        List<WebsiteProduct> websiteProducts = super.baseMapper.selectList(query);
        if (websiteProducts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> productIds = websiteProducts.stream().map(WebsiteProduct::getId).toList();
        LambdaQueryWrapper<WebsiteProductI18n> i18nQuery = Wrappers.lambdaQuery();
        i18nQuery.in(WebsiteProductI18n::getProductId, productIds);
        i18nQuery.eq(WebsiteProductI18n::getLocale, currentLocale.getCode());
        List<WebsiteProductI18n> websiteProductI18ns = websiteProductI18nMapper.selectList(i18nQuery);
        Map<Long, WebsiteProductI18n> i18nMap = websiteProductI18ns.stream().collect(Collectors.toMap(WebsiteProductI18n::getProductId, Function.identity(), (first, _) -> first));
        List<Long> missingProductIds = productIds.stream().filter(productId -> !i18nMap.containsKey(productId)).toList();
        if (!missingProductIds.isEmpty()) {
            SysLocale defaultLocale = SysLocale.defaultSysLocale();
            i18nQuery = Wrappers.lambdaQuery();
            i18nQuery.in(WebsiteProductI18n::getProductId, missingProductIds);
            i18nQuery.eq(WebsiteProductI18n::getLocale, defaultLocale.getCode());
            List<WebsiteProductI18n> defaultLocaleI18ns = websiteProductI18nMapper.selectList(i18nQuery);
            Map<Long, WebsiteProductI18n> defaultI18nMap = defaultLocaleI18ns.stream().collect(Collectors.toMap(WebsiteProductI18n::getProductId, Function.identity(), (first, _) -> first));
            i18nMap.putAll(defaultI18nMap);
        }

        List<WebsiteProductI18nVO> result = new ArrayList<>(websiteProducts.size());
        for (WebsiteProduct product : websiteProducts) {
            WebsiteProductI18n productI18n = i18nMap.get(product.getId());
            if (productI18n == null) {
                continue;
            }
            WebsiteProductI18nVO productView = this.convertWebsiteProduct(product, productI18n);
            result.add(productView);
        }
        List<Long> imageAttachmentIds = new ArrayList<>(result.size() * 2);
        for (WebsiteProductI18nVO product : result) {
            imageAttachmentIds.add(product.getCoverImageAttachmentId());
            imageAttachmentIds.add(product.getFeatureImageAttachmentId());
        }
        Map<Long, String> imageUrls = attachmentService.getAuthorizedUrls(imageAttachmentIds);
        for (WebsiteProductI18nVO product : result) {
            this.fillProductImageUrls(product, imageUrls);
        }
        return result;
    }

    @Override
    public WebsiteProductI18nVO websiteDetail(String slug) {
        LambdaQueryWrapper<WebsiteProduct> productQuery = Wrappers.lambdaQuery();
        productQuery.eq(WebsiteProduct::getSlug, slug);
        WebsiteProduct product = super.baseMapper.selectOne(productQuery);
        Assert.isNull(product, "website.product.not-found", "商品不存在");

        SysLocale currentLocale = I18nContext.getCurrentLocale();
        WebsiteProductI18n productI18n = this.findWebsiteProductI18n(product.getId(), currentLocale);
        Assert.isNull(productI18n, "website.product.not-found", "商品不存在");

        WebsiteProductI18nVO productView = this.convertWebsiteProduct(product, productI18n);
        Long productI18nId = productI18n.getId();
        LambdaQueryWrapper<WebsiteProductDetailItem> detailQuery = Wrappers.lambdaQuery();
        detailQuery.eq(WebsiteProductDetailItem::getProductI18nId, productI18nId);
        detailQuery.eq(WebsiteProductDetailItem::getIsShow, true);
        detailQuery.orderByAsc(WebsiteProductDetailItem::getSortOrder, WebsiteProductDetailItem::getId);
        List<WebsiteProductDetailItem> detailItems = websiteProductDetailItemMapper.selectList(detailQuery);
        List<WebsiteProductDetailItem> features = detailItems.stream().filter(item -> WebsiteProductEnum.DetailItemType.FEATURE.getCode().equals(item.getItemType())).toList();
        List<WebsiteProductDetailItem> specifications = detailItems.stream().filter(item -> WebsiteProductEnum.DetailItemType.SPECIFICATION.getCode().equals(item.getItemType())).toList();
        productView.setFeatures(features);
        productView.setSpecifications(specifications);

        LambdaQueryWrapper<WebsiteProductMediaItem> mediaQuery = Wrappers.lambdaQuery();
        mediaQuery.eq(WebsiteProductMediaItem::getProductI18nId, productI18nId);
        mediaQuery.eq(WebsiteProductMediaItem::getIsShow, true);
        mediaQuery.orderByAsc(WebsiteProductMediaItem::getSortOrder, WebsiteProductMediaItem::getId);
        List<WebsiteProductMediaItem> mediaItems = websiteProductMediaItemMapper.selectList(mediaQuery);
        List<WebsiteProductMediaItemVO> mediaViews = BeanConvertUtils.mapList(mediaItems, WebsiteProductMediaItemVO.class);

        List<Long> imageAttachmentIds = new ArrayList<>(16);
        imageAttachmentIds.add(productView.getCoverImageAttachmentId());
        imageAttachmentIds.add(productView.getFeatureImageAttachmentId());
        for (WebsiteProductMediaItemVO mediaView : mediaViews) {
            imageAttachmentIds.add(mediaView.getImageAttachmentId());
        }
        Map<Long, String> imageUrls = attachmentService.getAuthorizedUrls(imageAttachmentIds);
        this.fillProductImageUrls(productView, imageUrls);
        for (WebsiteProductMediaItemVO mediaView : mediaViews) {
            Long imageAttachmentId = mediaView.getImageAttachmentId();
            String imageUrl = imageAttachmentId == null ? null : imageUrls.get(imageAttachmentId);
            mediaView.setImageUrl(imageUrl);
        }

        List<WebsiteProductMediaItemVO> applications = mediaViews.stream().filter(item -> WebsiteProductEnum.MediaItemType.APPLICATION.getCode().equals(item.getItemType())).toList();
        List<WebsiteProductMediaItemVO> cases = mediaViews.stream().filter(item -> WebsiteProductEnum.MediaItemType.CASE.getCode().equals(item.getItemType())).toList();
        productView.setApplications(applications);
        productView.setCases(cases);
        return productView;
    }

    /**
     * 查询指定商品的当前语言内容，缺失时回退系统默认语言。
     */
    private WebsiteProductI18n findWebsiteProductI18n(Long productId, SysLocale currentLocale) {
        LambdaQueryWrapper<WebsiteProductI18n> i18nQuery = Wrappers.lambdaQuery();
        i18nQuery.eq(WebsiteProductI18n::getProductId, productId);
        i18nQuery.eq(WebsiteProductI18n::getLocale, currentLocale.getCode());
        WebsiteProductI18n productI18n = websiteProductI18nMapper.selectOne(i18nQuery);
        SysLocale defaultLocale = SysLocale.defaultSysLocale();
        if (productI18n != null || currentLocale == defaultLocale) {
            return productI18n;
        }

        i18nQuery = Wrappers.lambdaQuery();
        i18nQuery.eq(WebsiteProductI18n::getProductId, productId);
        i18nQuery.eq(WebsiteProductI18n::getLocale, defaultLocale.getCode());
        return websiteProductI18nMapper.selectOne(i18nQuery);
    }

    /**
     * 将商品及其国际化内容组装为官网返回对象。
     */
    private WebsiteProductI18nVO convertWebsiteProduct(
            WebsiteProduct product,
            WebsiteProductI18n productI18n
    ) {
        WebsiteProductI18nVO productView = BeanConvertUtils.convert(productI18n, WebsiteProductI18nVO.class);
        productView.setId(product.getId());
        productView.setProductI18nId(productI18n.getId());
        productView.setCategoryId(product.getCategoryId());
        productView.setSlug(product.getSlug());
        productView.setSortOrder(product.getSortOrder());
        productView.setIsShow(product.getIsShow());
        productView.setIsRecommended(product.getIsRecommended());
        return productView;
    }

    /**
     * 设置商品封面图和特点图的授权访问地址。
     */
    private void fillProductImageUrls(WebsiteProductI18nVO product, Map<Long, String> imageUrls) {
        Long coverImageAttachmentId = product.getCoverImageAttachmentId();
        String coverImageUrl = coverImageAttachmentId == null ? null : imageUrls.get(coverImageAttachmentId);
        product.setCoverImageUrl(coverImageUrl);
        Long featureImageAttachmentId = product.getFeatureImageAttachmentId();
        String featureImageUrl = featureImageAttachmentId == null ? null : imageUrls.get(featureImageAttachmentId);
        product.setFeatureImageUrl(featureImageUrl);
    }
}
