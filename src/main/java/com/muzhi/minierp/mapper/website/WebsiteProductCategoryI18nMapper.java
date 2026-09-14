package com.muzhi.minierp.mapper.website;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryI18nVO;

/**
 * <p>
 * 官网商品分类国际化 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
public interface WebsiteProductCategoryI18nMapper extends BaseMapper<WebsiteProductCategoryI18n> {

    /**
     * 列表查詢
     * @author Mr.Muzhi
     * @since 2026/9/13 14:58
     * @param page 分頁
     * @param query 查詢條件
     * @return 返回結果
     */
    IPage<WebsiteProductCategoryI18nVO> list(IPage<WebsiteProductCategoryI18nVO> page, WebsiteProductCategoryI18nVO query);
}
