package com.muzhi.minierp.service.website;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.website.WebsiteProductCategory;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryI18nVO;
import com.muzhi.minierp.vo.website.WebsiteProductCategoryVO;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * {@code IWebsiteProductCategoryService}: 官网商品分类 服务接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/11 20:42
 */
public interface IWebsiteProductCategoryService extends IService<WebsiteProductCategory> {

    /**
     * 在同一事务内新增商品分类及首种语言内容。
     */
    WebsiteProductCategoryI18n create(WebsiteProductCategoryVO query);

    /**
     * 为已有商品分类添加一种语言，重复语言拒绝新增。
     */
    WebsiteProductCategoryI18n addI18n(WebsiteProductCategoryI18n query);

    IPage<WebsiteProductCategoryI18nVO> list(WebsiteProductCategoryI18nVO query, @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam("pageSize") Integer pageSize);

}
