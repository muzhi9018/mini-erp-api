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
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * <p>
 * 正式报价单，保存客户、开单方及计价结果作为历史记录
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Getter
@Setter
@ToString
@TableName("quotation")
public class Quotation implements Serializable {

    @Serial
    private static final long serialVersionUID = -3099136105730867453L;
    
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
     * 正式报价单号，全表唯一，逻辑删除后不复用
     */
    @TableField("quote_number")
    private String quoteNumber;

    /**
     * 正式保存时按台北时区取得的编号日期
     */
    @TableField("number_date")
    private LocalDate numberDate;

    /**
     * 同一编号日的流水号，两种币种共用 1～999
     */
    @TableField("daily_sequence")
    private Short dailySequence;

    /**
     * 报价所属系统用户 ID，由登录身份写入
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 报价标题，默认报价单
     */
    @TableField("title")
    private String title;

    /**
     * 报价业务时间，可由用户指定
     */
    @TableField("quoted_at")
    private OffsetDateTime quotedAt;

    /**
     * 全单售价币种，见 {@link com.muzhi.minierp.enums.CurrencyEnum}
     */
    @TableField("currency")
    private String currency;

    /**
     * 是否已生成订单
     */
    @TableField("order_generated")
    private Boolean orderGenerated;

    /**
     * 报价版本
     */
    @TableField("version")
    private Long version;

    /**
     * 本单客户名称
     */
    @TableField("customer_name")
    private String customerName;

    /**
     * 本单客户联系人
     */
    @TableField("contact_name")
    private String contactName;

    /**
     * 本单客户联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 本单联系人电子邮件，预留字段
     */
    @TableField(value = "contact_email", select = false)
    private String contactEmail;

    /**
     * 本单收货地址
     */
    @TableField("shipping_address")
    private String shippingAddress;

    /**
     * 客户版可见的整单备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 正式保存时的开单方展示名称
     */
    @TableField("issuer_name")
    private String issuerName;

    /**
     * 正式保存时的开单方展示联络资料
     */
    @TableField("issuer_contact")
    private String issuerContact;

    /**
     * 各行原价合计
     */
    @TableField("original_amount")
    private BigDecimal originalAmount;

    /**
     * 各行成交额合计，即整单优惠前金额
     */
    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    /**
     * 整单应付比例，90 表示 9 折，与整单优惠后金额互斥
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 手填的整单优惠后金额，与整单折扣互斥
     */
    @TableField("discounted_amount")
    private BigDecimal discountedAmount;

    /**
     * 应用整单优惠后的最终报价金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;
}
