package com.muzhi.minierp.vo.product;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzhi.minierp.entity.product.ProductSku;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * SKU 管理详情及维护参数
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSku.class)
public class ProductSkuVO extends ProductSku {

    /**
     * 兼容 SKU 操作请求中的 SKU ID，仅用于入参
     */
    @JSONField(serialize = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long skuId;

    /**
     * 期望 SKU 行版本
     */
    private Long expectedVersion;

    /**
     * 所属商品名称
     */
    private String productName;

    /**
     * 所属商品编码
     */
    private String productCode;

    /**
     * 所属商品统一版本
     */
    private Long productVersion;

    /**
     * 计量单位名称
     */
    private String unitName;

    /**
     * 规格展示摘要
     */
    private String specificationText;

    /**
     * 当前 SKU 自身的图片列表
     */
    private List<ProductImageVO> images;

    /**
     * 结构化规格选择
     */
    private List<ProductSkuSpecValueVO> specValues;

    /**
     * 完整创建商品时按临时键选择规格候选值，仅用于入参。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ProductRequests.ClientSelection> selections;
}
