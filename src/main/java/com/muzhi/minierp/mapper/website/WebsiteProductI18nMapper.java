package com.muzhi.minierp.mapper.website;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzhi.minierp.entity.website.WebsiteProductI18n;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;

/**
 * <p>
 * 官网商品国际化 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
public interface WebsiteProductI18nMapper extends BaseMapper<WebsiteProductI18n> {

    /**
     * 商品列表查询
     * @author Mr.Muzhi
     * @since 2026/9/14 14:32
     * @param page 分页
     * @param query 条件
     * @return 返回结果
     */
    IPage<WebsiteProductI18nVO> list(Page<WebsiteProductI18nVO> page, WebsiteProductI18nVO query);
}
