package com.muzhi.minierp.vo.website;

import com.muzhi.minierp.entity.website.WebsiteProductCategory;
import com.muzhi.minierp.entity.website.WebsiteProductCategoryI18n;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * {@code WebsiteProductCategoryVO}: 官网商品分类视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers({@AutoMapper(target = WebsiteProductCategory.class)})
public class WebsiteProductCategoryVO extends WebsiteProductCategory {

    @Serial
    private static final long serialVersionUID = 843610920875623114L;

    /**
     * 分类国际化内容
     */
    private WebsiteProductCategoryI18n categoryI18n;
}
