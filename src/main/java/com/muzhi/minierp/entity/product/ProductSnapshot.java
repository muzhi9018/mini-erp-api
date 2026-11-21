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
 * SKU 商品版本快照，同一 SKU 与商品版本全表唯一，仅新增和查询，不随来源主档删除
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Getter
@Setter
@ToString
@TableName("product_snapshot")
public class ProductSnapshot implements Serializable {


    @Serial
    private static final long serialVersionUID = -3685072934641807790L;

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
     * 来源商品 ID，仅用于追溯，不依赖来源记录存在
     */
    @TableField("source_product_id")
    private Long sourceProductId;

    /**
     * 来源 SKU ID，仅用于追溯，与商品版本组成快照唯一键
     */
    @TableField("source_sku_id")
    private Long sourceSkuId;

    /**
     * 生成时的商品统一版本，由服务端显式写入，创建后不变，不作为快照行版本递增
     */
    @TableField("version")
    private Long version;

    /**
     * 生成时的来源 SKU 行版本，仅辅助追溯，不参与快照唯一键
     */
    @TableField("source_sku_version")
    private Long sourceSkuVersion;

    /**
     * 快照生成时的商品编码
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 快照生成时的 SKU 编码
     */
    @TableField("sku_code")
    private String skuCode;

    /**
     * 快照生成时的商品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 快照生成时的品牌名称
     */
    @TableField("brand")
    private String brand;

    /**
     * 快照生成时的 SKU 型号
     */
    @TableField("model")
    private String model;

    /**
     * 快照生成时的商品简要说明，纯文本
     */
    @TableField("description")
    private String description;

    /**
     * 快照生成时的类目编码
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 快照生成时的类目名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 快照生成时的完整类目名称路径
     */
    @TableField("category_path")
    private String categoryPath;

    /**
     * 快照生成时的 SKU 基本计量单位编码
     */
    @TableField("unit_code")
    private String unitCode;

    /**
     * 快照生成时的 SKU 基本计量单位名称
     */
    @TableField("unit_name")
    private String unitName;

    /**
     * 生成快照时来源 SKU 的数量小数位数，范围 0～6
     */
    @TableField("quantity_scale")
    private Integer quantityScale;

    /**
     * 快照生成时按显示顺序生成的规格摘要，无规格时为空字符串
     */
    @TableField("specification_text")
    private String specificationText;
}
