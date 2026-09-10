package com.muzhi.minierp.mapper.website;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.website.WebsiteProduct;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 官网商品 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
public interface WebsiteProductMapper extends BaseMapper<WebsiteProduct> {

    /** 添加语言时锁定父商品，串行检查同一商品的语言唯一性。 */
    WebsiteProduct selectByIdForUpdate(@Param("id") Long id);
}
