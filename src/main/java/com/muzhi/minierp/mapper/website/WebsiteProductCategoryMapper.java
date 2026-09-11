package com.muzhi.minierp.mapper.website;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.website.WebsiteProductCategory;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 官网商品分类 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
public interface WebsiteProductCategoryMapper extends BaseMapper<WebsiteProductCategory> {

    /**
     * 添加语言时锁定分类，串行检查同一分类的语言唯一性。
     */
    WebsiteProductCategory selectByIdForUpdate(@Param("id") Long id);
}
