package com.muzhi.minierp.dto.website;

import com.muzhi.minierp.entity.website.WebsiteProductDetailItem;
import com.muzhi.minierp.entity.website.WebsiteProductI18n;
import com.muzhi.minierp.entity.website.WebsiteProductMediaItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/** 一个语言的完整商品内容，不接收实体主键及审计字段。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebsiteProductI18nDTO extends WebsiteProductI18n {

    @Serial
    private static final long serialVersionUID = -593755304120421850L;

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
    private List<WebsiteProductMediaItem> applications;

    /**
     * 产品案列列表
     */
    private List<WebsiteProductMediaItem> cases;
}
