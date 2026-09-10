package com.muzhi.minierp.vo.website;

import com.muzhi.minierp.entity.website.WebsiteProduct;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * {@code WebsiteProductVO}: 官网商品视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/10 18:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers({@AutoMapper(target = WebsiteProduct.class)})
public class WebsiteProductVO extends WebsiteProduct {

    @Serial
    private static final long serialVersionUID = 7779697722714572636L;

    /**
     * 商品国际化
     */
    private WebsiteProductI18nVO productI18n;
}
