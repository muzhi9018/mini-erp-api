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
 * 内部商品 SKU，库存为人工维护的当前数量，不自动增减或预占
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_sku")
public class ProductSku implements Serializable {


    @Serial
    private static final long serialVersionUID = 2564344005141176959L;


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
     * 所属商品 ID（逻辑关联），创建后不直接修改
     */
    @TableField("product_id")
    private Long productId;

    /**
     * SKU 货号，全表唯一，逻辑删除后不复用
     */
    @TableField("code")
    private String code;

    /**
     * 厂家型号等型号信息
     */
    @TableField("model")
    private String model;

    /**
     * 基本计量单位 ID（逻辑关联），库存及后续单据数量使用此单位
     */
    @TableField("unit_id")
    private Long unitId;

    /**
     * 参考进价，系统统一业务币种，空表示未维护，0 表示零价
     */
    @TableField("reference_purchase_price")
    private BigDecimal referencePurchasePrice;

    /**
     * 参考售价，系统统一业务币种，空表示未维护，0 表示零价
     */
    @TableField("reference_sale_price")
    private BigDecimal referenceSalePrice;

    /**
     * 当前人工库存数量，非负，按基本单位精度校验，通过独立操作设置绝对值
     */
    @TableField("stock_quantity")
    private BigDecimal stockQuantity;

    /**
     * 服务端对按规格 ID 排序的规格 ID 与候选值 ID 列表规范化后计算的 SHA-256 小写十六进制摘要，无规格使用空列表
     */
    @TableField("specification_key")
    private String specificationKey;

    /**
     * SKU 状态：ENABLED=启用，DISABLED=停用，停用仍占用规格组合
     */
    @TableField("status")
    private String status;

    /**
     * 展示顺序，同值时按主键排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * SKU 资料、状态及库存操作共用的行版本，由应用校验并递增，同时递增所属商品统一版本，不作为快照唯一键
     */
    @TableField("version")
    private Long version;
}
