package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.enums.CurrencyEnum;
import lombok.Data;
import io.github.linpeilie.annotations.AutoMapper;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 业务选品结果，不含内部进价和管理审计字段
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@AutoMapper(target = ProductSkuVO.class)
public class ProductOptionVO {

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * SKU 货号
     */
    private String code;

    /**
     * 型号
     */
    private String model;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 数量精度
     */
    private Integer quantityScale;

    /**
     * 参考售价
     */
    private BigDecimal referenceSalePrice;

    /**
     * 参考售价币种，见 {@link CurrencyEnum}
     */
    private String referenceSaleCurrency;

    /**
     * 当前库存参考值
     */
    private BigDecimal stockQuantity;

    /**
     * 选品时的商品版本
     */
    private Long productVersion;

    /**
     * 选品时的 SKU 行版本
     */
    private Long skuVersion;

    /**
     * 当前 SKU 自身的图片列表
     */
    private List<ProductImageVO> images;

    /**
     * 规格展示摘要
     */
    private String specificationText;

    /**
     * 结构化规格
     */
    private List<ProductOptionSpecVO> specs;

}
