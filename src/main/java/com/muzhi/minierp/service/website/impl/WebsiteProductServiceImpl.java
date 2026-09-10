package com.muzhi.minierp.service.website.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import com.muzhi.minierp.entity.website.*;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.website.WebsiteProductCategoryMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductDetailItemMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductI18nMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductMediaItemMapper;
import com.muzhi.minierp.service.website.IWebsiteProductService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.website.WebsiteProductCreatedVO;
import com.muzhi.minierp.vo.website.WebsiteProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 官网商品新增与语言内容新增。 */
@Service
@RequiredArgsConstructor
public class WebsiteProductServiceImpl extends ServiceImpl<WebsiteProductMapper, WebsiteProduct> implements IWebsiteProductService {

    private final WebsiteProductCategoryMapper websiteProductCategoryMapper;

    private final WebsiteProductI18nMapper websiteProductI18nMapper;

    private final WebsiteProductDetailItemMapper websiteProductDetailItemMapper;

    private final WebsiteProductMediaItemMapper websiteProductMediaItemMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCreatedVO create(WebsiteProductVO query) {
        WebsiteProductCategory dbCategory = websiteProductCategoryMapper.selectById(query.getCategoryId());
        Assert.isNull(dbCategory, "商品分类不存在");
        String slug = query.getSlug();
        LambdaQueryWrapper<WebsiteProduct> slugQuery = Wrappers.lambdaQuery();
        slugQuery.eq(WebsiteProduct::getSlug, slug);
        Assert.isTrue(baseMapper.exists(slugQuery), "商品 slug 已存在");

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
        product.setIsDeleted(false);
        try {
            baseMapper.insert(product);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("商品 slug 已存在", exception);
        }

        return insertI18n(product.getId(), locale, query.getProductI18n());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCreatedVO addI18n(WebsiteProductI18nVO query) {
        Long productId = query.getProductId();
        String locale = query.getLocale();
        // 在事务内锁住父商品，避免两个添加语言请求同时通过重复检查。
        WebsiteProduct dbProduct = baseMapper.selectByIdForUpdate(productId);
        Assert.isNull(dbProduct, "商品不存在");
        LambdaQueryWrapper<WebsiteProductI18n> productI18nQuery = Wrappers.lambdaQuery();
        productI18nQuery.eq(WebsiteProductI18n::getProductId, productId);
        productI18nQuery.eq(WebsiteProductI18n::getLocale, locale);
        Assert.isTrue(websiteProductI18nMapper.exists(productI18nQuery), "该商品已配置此语言");
        return insertI18n(productId, locale, query);
    }

    private WebsiteProductCreatedVO insertI18n(Long productId, String locale, WebsiteProductI18nVO query) {
        WebsiteProductI18n translation = new WebsiteProductI18n();
        BeanUtils.copyProperties(query, translation);
        translation.setId(IdWorker.getId());
        translation.setProductId(productId);
        translation.setLocale(locale);
        translation.setName(query.getName().trim());
        translation.setIsDeleted(false);
        try {
            websiteProductI18nMapper.insert(translation);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("该商品已配置此语言", exception);
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
        List<WebsiteProductMediaItem> applications = query.getApplications();
        for (WebsiteProductMediaItem application : applications) {
           this.setDefaultMediaItemParams(application, translation.getId());
            mediaItems.add(application);
        }
        List<WebsiteProductMediaItem> cases = query.getCases();
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
    }





}
