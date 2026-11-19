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
 * 商品计量单位，供 SKU 数量和数值规格使用，不提供单位换算
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_unit")
public class ProductUnit implements Serializable {

    @Serial
    private static final long serialVersionUID = -4567155790716784704L;

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
     * 单位编码，全表唯一，逻辑删除后不复用
     */
    @TableField("code")
    private String code;

    /**
     * 单位名称，例如件、张、米、mm，未删除记录中唯一
     */
    @TableField("name")
    private String name;

    /**
     * 允许的数量小数位数，范围 0～6，已被 SKU 使用后不能直接修改
     */
    @TableField("quantity_scale")
    private Integer quantityScale;

    /**
     * 展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
