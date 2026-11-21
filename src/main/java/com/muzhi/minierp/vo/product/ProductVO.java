package com.muzhi.minierp.vo.product;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzhi.minierp.entity.product.Product;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 商品完整档案及维护参数
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Product.class)
public class ProductVO extends Product {

    /**
     * 兼容商品操作请求中的商品 ID，仅用于入参
     */
    @JSONField(serialize = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long productId;

    /**
     * 类目名称
     */
    private String categoryName;

    /**
     * 有效 SKU 数量
     */
    private Long skuCount;

    /**
     * 主图附件 ID，由图片列表派生
     */
    private Long coverImageAttachmentId;

    /**
     * 主图授权地址
     */
    private String coverImageUrl;

    /**
     * 编辑时的期望商品版本
     */
    private Long expectedVersion;

    /**
     * 完整图片列表
     */
    private List<ProductImageVO> images;

    /**
     * 结构化规格列表
     */
    private List<ProductSpecVO> specs;

    /**
     * 详情中的有效 SKU 列表
     */
    private List<ProductSkuVO> skus;
}
