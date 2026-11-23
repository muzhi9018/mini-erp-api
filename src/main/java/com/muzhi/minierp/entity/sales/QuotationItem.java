package com.muzhi.minierp.entity.sales;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * <p>
 * 正式报价商品明细，使用商品快照和保存时的成交数据
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Getter
@Setter
@ToString
@TableName("quotation_item")
public class QuotationItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -4362555348887401283L;
    
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private OffsetDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime gmtModified;

    /**
     * 创建者
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 更新者
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 是否删除(false:否; true:是)
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 所属报价单 ID（逻辑关联）
     */
    @TableField("quotation_id")
    private Long quotationId;

    /**
     * 商品快照 ID（逻辑关联），来源 SKU 通过快照追溯
     */
    @TableField("snapshot_id")
    private Long snapshotId;

    /**
     * 保存时的 SKU 售价币种，须与报价主表一致，见 {@link com.muzhi.minierp.enums.CurrencyEnum}
     */
    @TableField("currency")
    private String currency;

    /**
     * 报价数量，大于 0，由应用按当时 SKU 的数量精度校验
     */
    @TableField("quantity")
    private BigDecimal quantity;

    /**
     * 正式保存时的 SKU 参考售价
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 单价乘以数量后四舍五入至两位小数的行原价
     */
    @TableField("original_amount")
    private BigDecimal originalAmount;

    /**
     * 行应付比例，90 表示 9 折，与行优惠后金额互斥
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 手填的整行优惠后金额，与行折扣互斥
     */
    @TableField("discounted_amount")
    private BigDecimal discountedAmount;

    /**
     * 应用行优惠后的整行成交额
     */
    @TableField("line_amount")
    private BigDecimal lineAmount;

    /**
     * 客户版可见的商品行备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 商品行展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
