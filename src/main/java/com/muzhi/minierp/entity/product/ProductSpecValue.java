package com.muzhi.minierp.entity.product;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * <p>
 * 商品规格候选值，文本或数值与所属维度类型的一致性由应用校验
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_spec_value")
public class ProductSpecValue implements Serializable {


    @Serial
    private static final long serialVersionUID = -7548599475878236835L;

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
     * 所属规格维度 ID（逻辑关联）
     */
    @TableField("spec_id")
    private Long specId;

    /**
     * 文本候选值，去除首尾空白，与数值候选值必须且只能填写一个
     */
    @TableField("text_value")
    private String textValue;

    /**
     * 数值候选值，使用所属维度的单位，与文本候选值必须且只能填写一个
     */
    @TableField("number_value")
    private BigDecimal numberValue;

    /**
     * 服务端生成的去重值，文本去除首尾空白，数值按等价规则去除无效尾零
     */
    @TableField("normalized_value")
    private String normalizedValue;

    /**
     * 候选值展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
