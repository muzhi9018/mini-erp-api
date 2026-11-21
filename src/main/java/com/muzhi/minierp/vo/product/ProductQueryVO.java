package com.muzhi.minierp.vo.product;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 商品与 SKU 分页筛选条件
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
public class ProductQueryVO {

    /**
     * 关键字，匹配商品名称、编码及 SKU 货号、型号
     */
    private String keyword;

    /**
     * 类目 ID，查询包含全部后代类目
     */
    private Long categoryId;

    /**
     * 商品 ID，用于 SKU 查询
     */
    private Long productId;

    /**
     * 商品状态，见 {@link com.muzhi.minierp.enums.ProductEnum.Status}
     */
    private String status;

    /**
     * SKU 状态，见 {@link com.muzhi.minierp.enums.ProductEnum.Status}
     */
    private String skuStatus;

    /**
     * 多个条件使用全部满足语义
     */
    private List<SpecFilter> specs;

    /**
     * 文本候选值或数值区间筛选，数值区间与维度单位一致。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecFilter {

        /**
         * 所属规格维度 ID
         */
        private Long specId;

        /**
         * 文本规格候选值 ID，与数值区间二选一
         */
        private Long specValueId;

        /**
         * 数值下限，包含边界
         */
        private BigDecimal minimum;

        /**
         * 数值上限，包含边界
         */
        private BigDecimal maximum;
    }
}
