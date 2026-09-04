package com.muzhi.minierp.entity.website;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 官网商品媒体明细（应用场景、案例展示）
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("website_product_media_item")
public class WebsiteProductMediaItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -3034232540532124078L;

    /**
     * 主键
     */
    @TableId("id")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 创建用户 id
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 更新用户 id
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 是否已逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 商品国际化 id（逻辑关联）
     */
    @TableField("product_i18n_id")
    private Long productI18nId;

    /**
     * 明细类型：APPLICATION=应用场景，CASE=案例展示
     */
    @TableField("item_type")
    private String itemType;

    /**
     * 应用场景或案例标题
     */
    @TableField("title")
    private String title;

    /**
     * 应用场景或案例描述
     */
    @TableField("description")
    private String description;

    /**
     * 应用场景或案例图片地址
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 同类型内容展示顺序，数值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否在官网展示
     */
    @TableField("is_show")
    private Boolean isShow;
}
