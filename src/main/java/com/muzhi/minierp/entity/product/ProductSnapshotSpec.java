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
 * 商品快照规格明细，完整复制当时的名称、选中值和单位，仅新增和查询，不动态读取来源主档
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_snapshot_spec")
public class ProductSnapshotSpec implements Serializable {


    @Serial
    private static final long serialVersionUID = 7122653778260011433L;


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
     * 所属商品快照 ID（逻辑关联），与快照主记录在同一事务中完整新增
     */
    @TableField("snapshot_id")
    private Long snapshotId;

    /**
     * 来源规格维度 ID，仅用于追溯，同一快照内唯一
     */
    @TableField("source_spec_id")
    private Long sourceSpecId;

    /**
     * 来源规格候选值 ID，仅用于追溯，不依赖来源记录存在
     */
    @TableField("source_spec_value_id")
    private Long sourceSpecValueId;

    /**
     * 快照生成时的规格维度名称
     */
    @TableField("spec_name")
    private String specName;

    /**
     * 快照规格值类型：TEXT=文本，NUMBER=数值
     */
    @TableField("value_type")
    private String valueType;

    /**
     * 快照文本值，仅文本类型填写，与数值必须且只能填写一个
     */
    @TableField("text_value")
    private String textValue;

    /**
     * 快照数值，仅数值类型填写，使用快照保存的规格单位
     */
    @TableField("number_value")
    private BigDecimal numberValue;

    /**
     * 快照数值规格的单位编码，文本类型必须为空，与单位名称成对填写
     */
    @TableField("unit_code")
    private String unitCode;

    /**
     * 快照数值规格的单位名称，文本类型必须为空，与单位编码成对填写
     */
    @TableField("unit_name")
    private String unitName;

    /**
     * 快照生成时的显示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
