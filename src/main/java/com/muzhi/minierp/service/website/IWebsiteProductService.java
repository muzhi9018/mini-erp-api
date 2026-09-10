package com.muzhi.minierp.service.website;

import com.muzhi.minierp.entity.website.WebsiteProduct;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import com.muzhi.minierp.vo.website.WebsiteProductCreatedVO;
import com.muzhi.minierp.vo.website.WebsiteProductVO;

/**
 * <p>
 * 官网商品 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
public interface IWebsiteProductService extends IService<WebsiteProduct> {

    /** 在同一事务内新增商品、默认语言及其明细。 */
    WebsiteProductCreatedVO create(WebsiteProductVO query);

    /** 为已有商品新增一个语言及其明细，重复语言拒绝新增。 */
    WebsiteProductCreatedVO addI18n(WebsiteProductI18nVO query);
}
