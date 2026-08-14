package com.muzhi.minierp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户信息
 *   <tr><td>id</td><td>{@link Long}</td><td>id</td><td>BIGINT UNSIGNED</td><td>主键</td></tr>
 *   <tr><td>gmtCreate</td><td>{@link LocalDateTime}</td><td>gmt_create</td><td>DATETIME</td><td>创建时间</td></tr>
 *   <tr><td>gmtModified</td><td>{@link LocalDateTime}</td><td>gmt_modified</td><td>DATETIME</td><td>更新时间</td></tr>
 *   <tr><td>createUser</td><td>{@link Long}</td><td>create_user</td><td>BIGINT</td><td>创建者</td></tr>
 *   <tr><td>updateUser</td><td>{@link Long}</td><td>update_user</td><td>BIGINT</td><td>更新者</td></tr>
 *   <tr><td>isDeleted</td><td>{@link Boolean}</td><td>is_deleted</td><td>TINYINT UNSIGNED</td><td>是否删除(0:否; 1:是)</td></tr>
 *   <tr><td>status</td><td>{@link Integer}</td><td>status</td><td>TINYINT UNSIGNED</td><td>状态(1:正常; 0:禁用)</td></tr>
 *   <tr><td>username</td><td>{@link String}</td><td>username</td><td>VARCHAR(32)</td><td>用户名</td></tr>
 *   <tr><td>password</td><td>{@link String}</td><td>password</td><td>VARCHAR(256)</td><td>密码</td></tr>
 *   <tr><td>userNo</td><td>{@link String}</td><td>user_no</td><td>VARCHAR(32)</td><td>用户编号</td></tr>
 *   <tr><td>currentRoleCode</td><td>{@link String}</td><td>current_role_code</td><td>VARCHAR(32)</td><td>当前使用角色</td></tr>
 *   <tr><td>realName</td><td>{@link String}</td><td>real_name</td><td>VARCHAR(64)</td><td>用户昵称</td></tr>
 *   <tr><td>mobile</td><td>{@link String}</td><td>mobile</td><td>VARCHAR(32)</td><td>手机号</td></tr>
 *   <tr><td>remark</td><td>{@link String}</td><td>remark</td><td>VARCHAR(128)</td><td>备注</td></tr>
 *   <tr><td>sort</td><td>{@link Integer}</td><td>sort</td><td>INT</td><td>排序</td></tr>
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -5617078221886149740L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
    @TableField("is_deleted")
    @TableLogic
    private boolean deleted;

    /**
     * 状态(1:正常; 0:禁用)
     */
    @TableField("status")
    private Integer status;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 用户编号
     */
    @TableField("user_no")
    private String userNo;

    /**
     * 当前使用角色
     */
    @TableField("current_role_code")
    private String currentRoleCode;

    /**
     * 用户昵称
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

}
