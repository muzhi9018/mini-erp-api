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
 * 商品图片，同一商品下 SKU 共用，非空图片列表恰好一张主图由应用事务保证
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_image")
public class ProductImage implements Serializable {


    @Serial
    private static final long serialVersionUID = 766068913109213822L;


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
     * 图片附件 ID（逻辑关联），同一商品内未删除记录唯一，移除关联不删除附件对象
     */
    @TableField("attachment_id")
    private Long attachmentId;

    /**
     * 是否主图，同一商品最多一张有效主图，切换时先解除旧主图标记
     */
    @TableField("is_cover")
    private Boolean isCover;

    /**
     * 图片展示顺序，按目标列表顺序保存，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
