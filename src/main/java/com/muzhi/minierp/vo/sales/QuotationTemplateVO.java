package com.muzhi.minierp.vo.sales;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzhi.minierp.entity.sales.QuotationTemplate;
import com.muzhi.minierp.entity.sales.QuotationTemplateItem;
import com.muzhi.minierp.enums.QuotationEnum.Scope;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.ReverseAutoMapping;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 报价模板共用模型，范围、版本和可用性由服务端确定。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
@AutoMapper(target = QuotationTemplate.class, convertGenerate = false)
public class QuotationTemplateVO {

    /**
     * 模板 ID。
     */
    private Long id;

    /**
     * 修改模板时提交的期望版本。
     */
    @JSONField(serialize = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long expectedVersion;

    /**
     * 模板名称。
     */
    private String name;

    /**
     * 套用时填入报价表单的标题。
     */
    private String title;

    /**
     * 模板范围，见 {@link Scope}。
     */
    @JSONField(deserialize = false)
    private Scope scope;

    /**
     * 模板当前版本。
     */
    @JSONField(deserialize = false)
    private Long version;

    /**
     * 模板是否由当前登录用户创建。
     */
    @JSONField(deserialize = false)
    private Boolean ownedByCurrentUser;

    /**
     * 模板更新时间。
     */
    @JSONField(deserialize = false)
    private OffsetDateTime gmtModified;

    /**
     * 整单应付比例，90 表示 9 折。
     */
    @ReverseAutoMapping(ignore = true)
    @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
    private BigDecimal discountRate;

    /**
     * 套用时填入报价表单的整单备注。
     */
    @ReverseAutoMapping(ignore = true)
    private String remark;

    /**
     * 当前是否可套用。
     */
    @JSONField(deserialize = false)
    private Boolean usable;

    /**
     * 受商品异常影响的明细行数。
     */
    @JSONField(deserialize = false)
    private Integer issueCount;

    /**
     * 按模板顺序排列的商品明细。
     */
    private List<Item> items;

    /**
     * 模板明细的保存输入、实时选品资料和套用默认值。
     */
    @Data
    @AutoMapper(target = QuotationTemplateItem.class, convertGenerate = false)
    public static class Item {

        /**
         * 模板明细 ID。
         */
        @JSONField(deserialize = false)
        private Long id;

        /**
         * SKU ID，同一 SKU 可出现在多个位置。
         */
        private Long skuId;

        /**
         * 明细位置，从 0 开始。
         */
        @JSONField(deserialize = false)
        private Integer sortOrder;

        /**
         * 当前参考售价币种，见 {@link com.muzhi.minierp.enums.CurrencyEnum}。
         */
        @JSONField(deserialize = false)
        private String referenceSaleCurrency;

        /**
         * SKU 当前选品资料。
         */
        @JSONField(deserialize = false)
        private ProductOptionVO product;

        /**
         * 套用模板时使用的默认数量。
         */
        @JSONField(deserialize = false)
        private String quantity;

        /**
         * 本行应付比例。
         */
        @JSONField(serializeFeatures = {JSONWriter.Feature.WriteNonStringValueAsString, JSONWriter.Feature.WriteBigDecimalAsPlain})
        private BigDecimal discountRate;

        /**
         * 套用时填入报价表单的行备注。
         */
        private String remark;

        /**
         * 当前选品不可用的原因码。
         */
        @JSONField(deserialize = false)
        private List<String> issues;
    }
}
