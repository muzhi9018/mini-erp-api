package com.muzhi.minierp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.system.SysRolePermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色权限关联表 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {


    /**
     * 根据订单 id 删除
     * @author Mr.Muzhi
     * @since 2023/12/21 16:40
     * @param roleId 角色id
     * @return  返回结果
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据菜单 id 删除
     * @author Mr.Muzhi
     * @since 2023/12/21 19:50
     * @param menuId 菜单 id
     * @return 返回结果
     */
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 批量保存角色全新
     * @author Mr.Muzhi
     * @since 2023/12/21 19:38
     * @param permissions permissions
     * @return 返回结果
     */
    int insertBatch(@Param("list") List<SysRolePermission> permissions);

    /**
     * 根据菜单id更新角色权限权限码
     * @author Mr.Muzhi
     * @since 2023/12/21 20:26
     * @param permissionCode 权限码
     * @param menuId 菜单id
     */
    int updatePermissionCodeByMenuId(@Param("permissionCode") String permissionCode, @Param("menuId") Long menuId);

}
