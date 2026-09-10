package com.muzhi.minierp.dto.website;

import lombok.Data;

/** 新增官网商品：共用信息和默认语言内容。 */
@Data
public class WebsiteProductCreateDTO {

    private Long categoryId;

    /** 小写字母、数字及中划线组成的稳定路由标识。 */
    private String slug;

    private Integer sortOrder;

    private Boolean isRecommended;

    /** locale 可省略，由系统默认语言配置确定。 */
    private WebsiteProductI18nDTO productI18n;
}
