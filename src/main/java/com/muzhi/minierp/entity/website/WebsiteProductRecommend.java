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
 * 官网商品详情页定向推荐关联表
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("website_product_recommend")
public class WebsiteProductRecommend implements Serializable {

    @Serial
    private static final long serialVersionUID = 5683678455380556512L;

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
     * 当前详情页商品 id
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 被推荐商品 id
     */
    @TableField("recommend_product_id")
    private Long recommendProductId;

    /**
     * 推荐商品展示顺序，数值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
