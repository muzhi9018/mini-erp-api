package com.muzhi.minierp.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.entity.system.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * <p>
 * {@code SysUserVO}: 用户视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserVO extends SysUser {


    @Serial
    private static final long serialVersionUID = 1187674135965896500L;
    /**
     * 用户已授权的角色列表
     */
    private List<SysRole> roles;
}
