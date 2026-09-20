package com.muzhi.minierp.vo.website;

import com.muzhi.minierp.entity.website.WebsiteProductDetailItem;
import com.muzhi.minierp.entity.website.WebsiteProductI18n;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/** 一个语言的完整商品内容，不接收实体主键及审计字段。 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers({@AutoMapper(target = WebsiteProductI18n.class)})
public class WebsiteProductI18nVO extends WebsiteProductI18n {

    @Serial
    private static final long serialVersionUID = -593755304120421850L;

    /**
     * 图片授权访问地址，仅用于查询返回。
     */
    private String coverImageUrl;

    /**
     * 图片授权访问地址，仅用于查询返回。
     */
    private String featureImageUrl;

    /**
     * 商品国际化 ID，用于关联语言下的媒体明细。
     */
    private Long productI18nId;

    /**
     * 商品分类 id
     */
    private Long categoryId;

    /**
     * 商品分类名称
     */
    private String categoryName;

    /**
     * 官网详情页路由标识
     */
    private String slug;

    /**
     * 官网展示顺序，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 是否在官网展示
     */
    private Boolean isShow;

    /**
     * 是否用于全局推荐展示位
     */
    private Boolean isRecommended;

    /**
     * 产品特点与优势列表
     */
    private List<WebsiteProductDetailItem> features;

    /**
     * 产品技术参数列表
     */
    private List<WebsiteProductDetailItem> specifications;

    /**
     * 产品应用场景
     */
    private List<WebsiteProductMediaItemVO> applications;

    /**
     * 产品案列列表
     */
    private List<WebsiteProductMediaItemVO> cases;

}
