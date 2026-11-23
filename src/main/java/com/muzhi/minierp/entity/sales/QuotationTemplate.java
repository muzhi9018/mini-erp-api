package com.muzhi.minierp.entity.sales;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * <p>
 * 个人或公共报价模板，保存 SKU 组合及整单折扣
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Getter
@Setter
@ToString
@TableName("quotation_template")
public class QuotationTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = -8303253981542268363L;

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
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 套用模板时填入报价表单的标题
     */
    @TableField("title")
    private String title;

    /**
     * 模板范围，见 {@link com.muzhi.minierp.enums.QuotationEnum.Scope}
     */
    @TableField("scope")
    private String scope;

    /**
     * 个人模板所有者或公共模板创建者的系统用户 ID
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 模板版本，由应用在修改主表及明细时校验并递增
     */
    @TableField("version")
    private Long version;

    /**
     * 可选的整单应付比例，90 表示 9 折
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 套用模板时填入报价表单的整单备注
     */
    @TableField("remark")
    private String remark;
}
