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
 * 报价模板商品明细，同一 SKU 可在多个位置使用不同折扣
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Getter
@Setter
@ToString
@TableName("quotation_template_item")
public class QuotationTemplateItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -1313104960149352041L;

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
     * 所属模板 ID（逻辑关联）
     */
    @TableField("template_id")
    private Long templateId;

    /**
     * 当前商品 SKU ID（逻辑关联），来源删除后仍保留用于定位和修复
     */
    @TableField("sku_id")
    private Long skuId;

    /**
     * 商品行展示顺序，同一模板的有效明细位置唯一
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 可选的行应付比例，90 表示 9 折
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 套用模板时填入对应商品行的备注
     */
    @TableField("remark")
    private String remark;
}
