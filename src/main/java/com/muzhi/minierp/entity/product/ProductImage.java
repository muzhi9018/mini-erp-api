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
 * 商品与 SKU 共用的图片关联，按归属分别维护图片和主图
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
     * 图片的归属类型，见 {@link com.muzhi.minierp.enums.ProductEnum.ImageType}
     */
    @TableField("image_type")
    private String imageType;

    /**
     * 图片归属 ID（逻辑关联），按图片类型存商品 ID 或 SKU ID
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 图片附件 ID（逻辑关联），同一商品或 SKU 的有效图片不可重复绑定
     */
    @TableField("attachment_id")
    private Long attachmentId;

    /**
     * 是否该商品或 SKU 的主图，每个归属最多一张有效主图
     */
    @TableField("is_cover")
    private Boolean isCover;

    /**
     * 图片展示顺序，按目标列表顺序保存，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
