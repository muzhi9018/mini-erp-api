package com.muzhi.minierp.vo.website;

import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * {@code WebsiteProductCategoryI18nVO}: 商品分類 i18n 視圖對象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/13 15:03
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebsiteProductCategoryI18nVO extends WebsiteProductCategoryI18n {

    @Serial
    private static final long serialVersionUID = 6525507408004475199L;

    /**
     * 图片授权访问地址，仅用于查询返回。
     */
    private String imageUrl;

    /**
     * 稳定的分类编码
     */
    private String code;

    /**
     * 官网展示顺序，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 是否在官网展示
     */
    private Boolean isShow;
}
