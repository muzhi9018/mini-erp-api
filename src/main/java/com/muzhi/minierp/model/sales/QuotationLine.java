package com.muzhi.minierp.model.sales;

import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.vo.product.ProductOptionVO;

/**
 * 报价明细及其当前选品或历史快照的展示资料。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationLine(
        QuotationItem item, Long skuId, String skuCode, String productName, String model,
        String specificationText, String unitName, int quantityScale
) {

    /**
     * 从当前选品资料生成预览行。
     */
    public static QuotationLine fromOption(QuotationItem item, ProductOptionVO option) {
        return new QuotationLine(item, option.getSkuId(), option.getCode(), option.getProductName(), option.getModel(),
                option.getSpecificationText(), option.getUnitName(), option.getQuantityScale());
    }

    /**
     * 从商品快照生成正式保存或历史查询的展示行。
     */
    public static QuotationLine fromSnapshot(QuotationItem item, ProductSnapshot snapshot) {
        return new QuotationLine(item, snapshot.getSourceSkuId(), snapshot.getSkuCode(), snapshot.getProductName(),
                snapshot.getModel(), snapshot.getSpecificationText(), snapshot.getUnitName(), snapshot.getQuantityScale());
    }

    /**
     * 沿用原有展示资料并替换本次重新计算的明细。
     */
    public QuotationLine withItem(QuotationItem item) {
        return new QuotationLine(item, this.skuId, this.skuCode, this.productName,
                this.model, this.specificationText, this.unitName, this.quantityScale);
    }
}
