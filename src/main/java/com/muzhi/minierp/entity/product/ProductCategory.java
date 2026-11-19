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
 * 内部商品类目
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_category")
public class ProductCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 2013341766365584056L;

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
     * 父类目 ID，顶级类目为 0（逻辑关联）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 类目编码，全表唯一，逻辑删除后不复用
     */
    @TableField("code")
    private String code;

    /**
     * 类目名称，未删除记录中同父级唯一
     */
    @TableField("name")
    private String name;

    /**
     * 展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 类目备注
     */
    @TableField("remark")
    private String remark;
}
