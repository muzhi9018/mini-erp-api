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
 * 官网商品分类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("website_product_category")
public class WebsiteProductCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1977631598730341972L;

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
     * 稳定的分类编码
     */
    @TableField("code")
    private String code;

    /**
     * 官网展示顺序，数值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否在官网展示
     */
    @TableField("is_show")
    private Boolean isShow;
}
