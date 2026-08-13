package com.muzhi.minierp.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Mr.Muzhi
 * @since 2021-06-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysRolePermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -149239156516681498L;

    /**
     * id
     */
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

    /**
     * 创建者
     */
    private Long createUser;

    /**
     * 更新者
     */
    private Long updateUser;

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 菜单id
     */
    private Long menuId;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 权限码
     */
    private String permissionCode;

}
