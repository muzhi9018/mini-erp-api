package com.muzhi.minierp.service.website;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.website.WebsiteProduct;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.enums.SysLocale;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import com.muzhi.minierp.vo.website.WebsiteProductCreatedVO;
import com.muzhi.minierp.vo.website.WebsiteProductVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

    /** 修改商品及指定语言的内容，语言标识保持不变。 */
    void update(WebsiteProductI18nVO query);

    /**
     * 删除商品及其语言内容、文本明细、媒体明细和推荐关系。
     *
     * @author Mr.Muzhi
     * @since 2026/9/23
     * @param productId 商品 ID
     */
    void delete(Long productId);

    IPage<WebsiteProductI18nVO> list(WebsiteProductI18nVO query, Integer pageNum, Integer pageSize);

    /**
     * 根据商品 ID 和语言编码查询编辑回显详情，不回退到默认语言，并包含未展示的明细。
     *
     * @author Mr.Muzhi
     * @since 2026/9/22
     * @param productId 商品 ID
     * @param locale 语言编码
     * @return 指定语言的商品详情
     */
    WebsiteProductI18nVO detail(Long productId, String locale);

    /**
     * 官網列表
     * @author Mr.Muzhi
     * @since 2026/9/20 20:54
     * @param categoryId 分類 id
     * @param recommended 是否推荐的
     * @return 列表數據
     */
    List<WebsiteProductI18nVO> websiteList(Long categoryId, Boolean recommended);

    /**
     * 根据 slug 查询官网商品详情，当前语言不存在时使用系统默认语言。
     *
     * @author Mr.Muzhi
     * @since 2026/9/20
     * @param slug 官网详情页路由标识
     * @return 官网商品详情
     */
    WebsiteProductI18nVO websiteDetail(String slug);

    /**
     * 商品中的语言列表
     * @author Mr.Muzhi
     * @since 2026/9/22 14:20
     * @param productId 商品id
     * @return 返回结果
     */
    List<SysLocale.Bean> productLocales(Long productId);
}
