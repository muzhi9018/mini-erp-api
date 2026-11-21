package com.muzhi.minierp.entity.product;

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
import java.time.OffsetDateTime;

/**
 * <p>
 * 内部商品档案，与官网商品独立维护
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = -3500595670057045908L;

    /**
     * 主键
     */
    @TableId("id")
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
     * 所属商品类目 ID（逻辑关联），允许任意层级的有效类目
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品编码，全表唯一，逻辑删除后不复用
     */
    @TableField("code")
    private String code;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 品牌名称
     */
    @TableField("brand")
    private String brand;

    /**
     * 商品简要说明，纯文本
     */
    @TableField("description")
    private String description;

    /**
     * 商品状态，见 {@link com.muzhi.minierp.enums.ProductEnum.Status}
     */
    @TableField("status")
    private String status;

    /**
     * 展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 商品统一版本，商品、图片、规格、SKU 及影响展示的共享资料更新时由应用在同一事务中递增，一次操作每款商品只加 1
     */
    @TableField("version")
    private Long version;
}
