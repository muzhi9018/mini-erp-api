package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductOptionSpecVO;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import com.muzhi.minierp.vo.product.ProductSnapshotImageVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 报价明细及对应商品的冻结展示资料。
 *
 * @author Mr.Muzhi
 * @since 2026-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers({
        @AutoMapper(target = QuotationItem.class, convertGenerate = false),
        @AutoMapper(target = QuotationItemVO.class, reverseConvertGenerate = false),
        @AutoMapper(target = ProductOptionVO.class, convertGenerate = false)
})
public class QuotationItemVO extends QuotationItem {

    @Serial
    private static final long serialVersionUID = -7662633674817066560L;

    /**
     * 来源 SKU ID
     */
    private Long skuId;

    /**
     * 新选商品确认的商品版本
     */
    private Long productVersion;

    /**
     * 新选商品确认的 SKU 行版本
     */
    private Long skuVersion;

    /**
     * SKU 货号
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 型号
     */
    private String model;

    /**
     * 规格展示摘要
     */
    private String specificationText;

    /**
     * 有序规格明细，新选行使用当前规格，历史行使用冻结规格
     */
    private List<ProductOptionSpecVO> specs;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 数量精度
     */
    private Integer quantityScale;

    /**
     * 客户版主图授权地址，优先 SKU 主图；历史报价使用快照主图，无图时为空
     */
    private String productImageUrl;

    /**
     * 保存时的商品及 SKU 图片，无图片时为空列表
     */
    private List<ProductSnapshotImageVO> images;

    /**
     * 从当前选品资料生成可提交的报价明细，数量及优惠由调用方补充。
     */
    public static QuotationItemVO fromOption(ProductOptionVO option) {
        QuotationItemVO item = BeanConvertUtils.convert(option, QuotationItemVO.class);
        item.setSkuCode(option.getCode());
        item.setUnitPrice(option.getReferenceSalePrice());
        item.setCurrency(option.getReferenceSaleCurrency());
        List<ProductSnapshotImageVO> images = item.getImages();
        for (int index = 0; index < images.size(); index++) {
            ProductSnapshotImageVO image = images.get(index);
            image.setImageSource(option.getImages().get(index).getImageType());
            image.setSourceImageId(option.getImages().get(index).getId());
        }
        return item;
    }
}
