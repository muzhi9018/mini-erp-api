package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.muzhi.minierp.enums.VaultEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * <p>
 * 密码库站点
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-28
 */
@Getter
@Setter
@ToString
@TableName("vault_site")
public class VaultSite implements Serializable {


    @Serial
    private static final long serialVersionUID = -3720520178743847908L;

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
     * 站点状态，参见 {@link VaultEnum.SiteStatus}
     */
    @TableField("status")
    private Short status;

    /**
     * 站点名称
     */
    @TableField("site_name")
    private String siteName;

    /**
     * 站点域名
     */
    @TableField("site_domain")
    private String siteDomain;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}
