package com.muzhi.minierp.entity.product;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * <p>
 * SKU 规格选择关系，商品归属和维度选择完整性由应用事务保证
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_sku_spec_value")
public class ProductSkuSpecValue implements Serializable {


    @Serial
    private static final long serialVersionUID = 8929942227505430603L;

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
     * SKU ID（逻辑关联）
     */
    @TableField("sku_id")
    private Long skuId;

    /**
     * 规格维度 ID（逻辑关联），未删除记录中同一 SKU 的每个维度只选一个值
     */
    @TableField("spec_id")
    private Long specId;

    /**
     * 规格候选值 ID（逻辑关联），必须属于所选维度且与 SKU 属于同一商品
     */
    @TableField("spec_value_id")
    private Long specValueId;
}
