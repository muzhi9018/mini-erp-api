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
 * 商品快照图片明细，完整保留当时的全部图片、主图标记和顺序，仅新增和查询，不随来源图片删除
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_snapshot_image")
public class ProductSnapshotImage implements Serializable {


    @Serial
    private static final long serialVersionUID = -4103741138806144915L;

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
     * 所属商品快照 ID（逻辑关联），与快照主表和规格明细在同一事务中完整新增
     */
    @TableField("snapshot_id")
    private Long snapshotId;

    /**
     * 来源商品图片关联 ID，仅用于追溯，不依赖来源记录存在
     */
    @TableField("source_image_id")
    private Long sourceImageId;

    /**
     * 快照生成时的图片附件 ID，同一快照内唯一，附件记录及实际对象必须保留
     */
    @TableField("attachment_id")
    private Long attachmentId;

    /**
     * 快照生成时是否主图，同一快照最多一张主图，非空列表恰好一张由应用保证
     */
    @TableField("is_cover")
    private Boolean isCover;

    /**
     * 快照生成时按当前图片顺序连续赋值的展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
