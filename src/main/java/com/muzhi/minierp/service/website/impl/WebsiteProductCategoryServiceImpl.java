package com.muzhi.minierp.service.website.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.website.WebsiteProductCategory;
import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.website.WebsiteProductCategoryI18nMapper;
import com.muzhi.minierp.mapper.website.WebsiteProductCategoryMapper;
import com.muzhi.minierp.service.website.IWebsiteProductCategoryService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * {@code WebsiteProductCategoryServiceImpl}: 官网商品分类 服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/11 20:43
 */
@Service
@RequiredArgsConstructor
public class WebsiteProductCategoryServiceImpl extends ServiceImpl<WebsiteProductCategoryMapper, WebsiteProductCategory> implements IWebsiteProductCategoryService {

    private final WebsiteProductCategoryI18nMapper websiteProductCategoryI18nMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCategoryI18n create(WebsiteProductCategoryVO query) {
        String code = query.getCode().trim();
        LambdaQueryWrapper<WebsiteProductCategory> codeQuery = Wrappers.lambdaQuery();
        codeQuery.eq(WebsiteProductCategory::getCode, code);
        boolean codeExisted = super.baseMapper.exists(codeQuery);
        Assert.isTrue(codeExisted, "website.product-category.code-already-exists", "商品分类编码已存在");

        int defaultSortOrder = 0;
        Integer requestedSortOrder = query.getSortOrder();
        int sortOrder = requestedSortOrder == null ? defaultSortOrder : requestedSortOrder;
        Long categoryId = IdWorker.getId();
        WebsiteProductCategory category = BeanConvertUtils.convert(query, WebsiteProductCategory.class);
        category.setId(categoryId);
        category.setCode(code);
        category.setSortOrder(sortOrder);
        // 与商品新增一致，分类创建后默认不展示。
        category.setIsShow(false);
        category.setIsDeleted(false);
        // 清除请求中的审计字段，交由自动填充处理。
        category.setGmtCreate(null);
        category.setGmtModified(null);
        category.setCreateUser(null);
        category.setUpdateUser(null);
        try {
            super.baseMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("website.product-category.code-already-exists", "商品分类编码已存在", exception);
        }

        return this.insertI18n(categoryId, query.getCategoryI18n());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteProductCategoryI18n addI18n(WebsiteProductCategoryI18n query) {
        Long categoryId = query.getWebsiteProductCategoryId();
        // 在事务内锁住分类，避免并发添加同一种语言。
        WebsiteProductCategory category = super.baseMapper.selectByIdForUpdate(categoryId);
        Assert.isNull(category, "website.product-category.not-found", "商品分类不存在");

        LambdaQueryWrapper<WebsiteProductCategoryI18n> localeQuery = Wrappers.lambdaQuery();
        localeQuery.eq(WebsiteProductCategoryI18n::getWebsiteProductCategoryId, categoryId);
        localeQuery.eq(WebsiteProductCategoryI18n::getLocale, query.getLocale());
        boolean localeExisted = websiteProductCategoryI18nMapper.exists(localeQuery);
        Assert.isTrue(localeExisted, "website.product-category.locale-already-exists", "该商品分类已配置此语言");

        return this.insertI18n(categoryId, query);
    }

    /**
     * 保存分类语言内容，关联 ID 由业务入口指定。
     */
    private WebsiteProductCategoryI18n insertI18n(Long categoryId, WebsiteProductCategoryI18n translation) {
        Long categoryI18nId = IdWorker.getId();
        String name = translation.getName().trim();
        translation.setId(categoryI18nId);
        translation.setWebsiteProductCategoryId(categoryId);
        translation.setName(name);
        translation.setIsDeleted(false);
        // 清除请求中的审计字段，交由自动填充处理。
        translation.setGmtCreate(null);
        translation.setGmtModified(null);
        translation.setCreateUser(null);
        translation.setUpdateUser(null);
        try {
            websiteProductCategoryI18nMapper.insert(translation);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("website.product-category.locale-already-exists", "该商品分类已配置此语言", exception);
        }
        return translation;
    }
}
