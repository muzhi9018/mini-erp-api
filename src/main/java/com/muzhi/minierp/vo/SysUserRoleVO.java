package com.muzhi.minierp.vo;

import com.muzhi.minierp.entity.system.SysUserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * <p>
 * {@code SysUserRoleVO}：用户角色授权信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserRoleVO extends SysUserRole {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 本次选择的角色 ID 列表，空列表清空可修改的角色
     * 自身原有且权限不低于当前登录角色的角色自动补齐
     */
    private List<Long> roleIds;
}
