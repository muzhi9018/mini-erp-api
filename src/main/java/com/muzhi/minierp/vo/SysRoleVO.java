package com.muzhi.minierp.vo;

import com.muzhi.minierp.entity.system.SysRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * <p>
 * {@code SysRoleVO}: 角色视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/12/19 10:56
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleVO extends SysRole {

    @Serial
    private static final long serialVersionUID = 2646733767154386137L;


    /**
     * 是否为系统默认角色
     */
    private boolean sysDefRole;

    /**
     * 菜单 id 列表
     */
    private List<Long> menuIds;

}
