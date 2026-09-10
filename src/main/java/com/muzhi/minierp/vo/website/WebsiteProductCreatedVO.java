package com.muzhi.minierp.vo.website;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 新增商品或语言后的标识，供前端继续编辑使用。 */
@Getter
@AllArgsConstructor
public class WebsiteProductCreatedVO {

    private final Long productId;

    private final Long productI18nId;

    private final String locale;
}
