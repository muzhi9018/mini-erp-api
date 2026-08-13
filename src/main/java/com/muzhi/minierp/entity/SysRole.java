package com.muzhi.minierp.entity;

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
 * 系统角色信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Getter
@Setter
@ToString
@TableName("sys_role")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = -6823149041248962685L;

    /**
     * id
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
     * 是否删除(0:否; 1:是)
     */
    @TableLogic
    @TableField("is_deleted")
    private boolean deleted;

    /**
     * 状态（1:正常; 0:禁用）
     */
    @TableField("status")
    private Short status;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色编码
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
