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
 * 商品规格维度，按商品定义
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_spec")
public class ProductSpec implements Serializable {


    @Serial
    private static final long serialVersionUID = -195578896163009149L;

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
     * 所属商品 ID（逻辑关联）
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 规格维度名称，未删除记录中同一商品内唯一
     */
    @TableField("name")
    private String name;

    /**
     * 规格值类型：TEXT=文本，NUMBER=数值
     */
    @TableField("value_type")
    private String valueType;

    /**
     * 数值规格计量单位 ID（逻辑关联），可为空，文本规格必须为空
     */
    @TableField("unit_id")
    private Long unitId;

    /**
     * 规格显示顺序，不参与 SKU 组合标识计算
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
