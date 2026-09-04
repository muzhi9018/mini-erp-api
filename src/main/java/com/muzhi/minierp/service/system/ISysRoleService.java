package com.muzhi.minierp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.dto.SysRolePermissionDTO;
import com.muzhi.minierp.entity.system.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.system.SysRolePermission;
import com.muzhi.minierp.vo.SysMenuVO;
import com.muzhi.minierp.vo.SysRoleVO;

import java.util.List;

/**
 * <p>
 * 系统角色信息 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 添加角色
     * @author Mr.Muzhi
     * @since 2023/12/19 10:47
     * @param sysRole 角色对象
     */
    void addRole(SysRole sysRole);

    /**
     * 删除角色
     * @author Mr.Muzhi
     * @since 2023/12/19 16:57
     * @param sysRole 删除对象
     */
    void delRole(SysRole sysRole);

    /**
     * 编辑角色
     * @author Mr.Muzhi
     * @since 2023/12/19 17:11
     * @param sysRole 编辑对象
     */
    void editRole(SysRole sysRole);

    /**
     * 角色分页查询
     * @author Mr.Muzhi
     * @since 2023/12/19 10:47
     * @param current 当前页
     * @param pageSize 一页数量
     * @param query 查询条件
     * @return 返回结果
     */
    IPage<SysRoleVO> findByPage(Integer current, Integer pageSize, SysRoleVO query);

    /**
     * 角色菜单详情
     * @author Mr.Muzhi
     * @since 2023/12/20 17:35
     * @param id 角色id
     * @return 返回结果
     */
    List<SysMenuVO> roleMenuDetail(Long id);

    /**
     * 角色授权
     * @author Mr.Muzhi
     * @since 2023/12/21 16:26
     * @param sysRole 角色对象
     */
    void roleAuthorize(SysRoleVO sysRole);

    /**
     * 根据 menuId 删除角色权限数据
     * @author Mr.Muzhi
     * @since 2023/12/21 20:16
     * @param menuId menuId
     */
    void deleteRolePermissionByMenuId(Long menuId);

    /**
     * 同步角色权限权限码
     * @author Mr.Muzhi
     * @since 2023/12/21 20:24
     * @param sysRolePermissionDTO 同步对象
     */
    void syncRolePermissionPermissionCode(SysRolePermissionDTO sysRolePermissionDTO);


    /**
     * 根据角色编码获取角色权限
     * @author Mr.Muzhi
     * @since 2026/8/14 10:42
     * @param roleCode 角色编码
     * @return 返回结果
     */
    List<SysRolePermission> loadPermissionsByRoleCode(String roleCode);
}
