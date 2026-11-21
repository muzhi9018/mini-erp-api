package com.muzhi.minierp.vo.product;

import com.alibaba.fastjson2.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.muzhi.minierp.entity.product.ProductImage;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 商品图片视图
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductImage.class)
@JSONType(ignores = {"gmtCreate", "gmtModified", "createUser", "updateUser", "isDeleted"})
@JsonIgnoreProperties({"gmtCreate", "gmtModified", "createUser", "updateUser", "isDeleted"})
public class ProductImageVO extends ProductImage {

    /**
     * 图片授权地址
     */
    private String url;
}
