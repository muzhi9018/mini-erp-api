package com.muzhi.minierp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzhi.minierp.entity.system.SysMenu;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.entity.system.SysRolePermission;
import com.muzhi.minierp.enums.DefaultRoleEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.system.SysMenuMapper;
import com.muzhi.minierp.mapper.system.SysRoleMapper;
import com.muzhi.minierp.mapper.system.SysRolePermissionMapper;
import com.muzhi.minierp.mapper.system.SysUserRoleMapper;
import com.muzhi.minierp.service.system.ISysRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.SysMenuVO;
import com.muzhi.minierp.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 系统角色信息 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final SysMenuMapper sysMenuMapper;

    @Override
    @Transactional
    public void addRole(SysRole sysRole) {
        LambdaQueryWrapper<SysRole> query = new QueryWrapper<SysRole>().lambda().eq(SysRole::getRoleCode, sysRole.getRoleCode());
        List<SysRole> dbRoles = baseMapper.selectList(query);
        Assert.isNotEmpty(dbRoles, "system.role.code-already-exists", "角色编码已存在");
        baseMapper.insert(sysRole);
    }

    @Override
    @Transactional
    public void delRole(SysRole sysRole) {
        SysRole dbRole = baseMapper.selectById(sysRole.getId());
        Assert.isNull(dbRole, "system.role.not-found", "角色不存在");
        DefaultRoleEnum defaultRole = DefaultRoleEnum.ofCode(dbRole.getRoleCode());
        Assert.isNotNull(defaultRole, "system.role.default-cannot-delete", "系统默认角色不能删除");
        // 判断当前角色有没有绑定别的用户
        Long bindUserCount = sysUserRoleMapper.countByRoleId(dbRole.getId());
        if (bindUserCount != null && bindUserCount > 0) {
            throw new BusinessException("system.role.has-users", "当前角色已绑定用户,请先移除后再删除");
        }
        // 逻辑删除，防止唯一主键冲突
        dbRole.setRoleCode(dbRole.getRoleCode() + ":" + dbRole.getId());
        baseMapper.updateById(dbRole);
        baseMapper.deleteById(sysRole.getId());
        // 删除角色权限
        sysRolePermissionMapper.deleteByRoleId(dbRole.getId());
    }

    @Override
    @Transactional
    public void editRole(SysRole sysRole) {
        SysRole dbRole = baseMapper.selectById(sysRole.getId());
        Assert.isNull(dbRole, "system.role.not-found", "角色不存在");
        DefaultRoleEnum defaultRole = DefaultRoleEnum.ofCode(dbRole.getRoleCode());
        SysRole updateRole = new SysRole();
        updateRole.setId(dbRole.getId());
        updateRole.setRoleName(sysRole.getRoleName());
        updateRole.setSort(sysRole.getSort());
        updateRole.setRemark(sysRole.getRemark());
        // 不是系统默认角色可以修改名称和状态
        if (defaultRole == null) {
            updateRole.setRoleName(sysRole.getRoleName());
            updateRole.setStatus(sysRole.getStatus());
        }
        baseMapper.updateById(updateRole);
    }

    @Override
    public IPage<SysRoleVO> findByPage(Integer current, Integer pageSize, SysRoleVO query) {
        IPage<SysRoleVO> page = new Page<>(current, pageSize);
        baseMapper.findByPage(page, query);
        for (SysRoleVO sysRole : page.getRecords()) {
            DefaultRoleEnum defaultRole = DefaultRoleEnum.ofCode(sysRole.getRoleCode());
            sysRole.setSysDefRole(defaultRole != null);
        }
        return page;
    }

    @Override
    public List<SysMenuVO> roleMenuDetail(Long id) {
        List<SysMenu> allMenuList = sysMenuMapper.selectList(Wrappers.emptyWrapper());
        List<SysMenuVO> menus = BeanConvertUtils.mapList(allMenuList, SysMenuVO.class);
        List<SysRolePermission> permissions = sysRolePermissionMapper.selectList(new QueryWrapper<SysRolePermission>().lambda().eq(SysRolePermission::getRoleId, id));
        List<Long> ownedMenuIds = permissions.stream().map(SysRolePermission::getMenuId).toList();
        List<SysMenuVO> oneNodes = menus.stream().filter(m -> m.getParentId().equals(0L)).toList();
        this.builderRoleMenuTree(oneNodes, menus, ownedMenuIds);
        return oneNodes;
    }

    /**
     * 封装菜单树形列表
     * @author Mr.Muzhi
     * @since 2023/12/21 19:25
     * @param currentNodes 当前级别列表
     * @param menus 所有菜单列表
     * @param ownedMenuIds 所拥有的菜单id
     */
    private void builderRoleMenuTree(List<SysMenuVO> currentNodes, List<SysMenuVO> menus, List<Long> ownedMenuIds) {
        for (SysMenuVO currentNode : currentNodes) {
            // 判断是否选中
            currentNode.setSelected(ownedMenuIds.contains(currentNode.getId()));
            // 寻找子节点
            List<SysMenuVO> children = menus.stream().filter(m -> m.getParentId().equals(currentNode.getId())).toList();
            this.builderRoleMenuTree(children, menus, ownedMenuIds);
            if (!children.isEmpty()) {
                currentNode.setChildren(children);
            }
        }
    }


    @Override
    @Transactional
    public void roleAuthorize(SysRoleVO sysRole) {
        Long roleId = sysRole.getId();
        SysRole dbRole = baseMapper.selectById(roleId);
        Assert.isNull(dbRole, "system.role.not-found", "角色不存在");
        List<Long> menuIds = sysRole.getMenuIds();
        sysRolePermissionMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<SysMenu> allMenuList = sysMenuMapper.findByIds(menuIds);
        List<SysMenuVO> menus = BeanConvertUtils.mapList(allMenuList, SysMenuVO.class);
        // 组装成树节点，防止授权链断裂
        List<SysMenuVO> oneNodes = menus.stream().filter(m -> m.getParentId().equals(0L)).toList();
        this.builderRoleMenuTree(oneNodes, menus, new ArrayList<>(16));
        List<SysRolePermission> permissions = new ArrayList<>(16);
        this.builderPermissions(oneNodes, permissions, dbRole);
        if (!permissions.isEmpty()) {
            sysRolePermissionMapper.insertBatch(permissions);
        }
    }

    /**
     * 构造权限列表
     * @author Mr.Muzhi
     * @since 2023/12/21 19:44
     * @param menus 菜单列表
     * @param permissions 权限列表容器
     * @param role 角色对象
     */
    private void builderPermissions(List<SysMenuVO> menus, List<SysRolePermission> permissions, SysRole role) {
        for (SysMenuVO menu : menus) {
            SysRolePermission permission = new SysRolePermission();
            permission.setId(IdWorker.getId());
            permission.setRoleCode(role.getRoleCode());
            permission.setRoleId(role.getId());
            permission.setMenuId(menu.getId());
            permission.setPermissionCode(menu.getPermissionCode() == null ? "" : menu.getPermissionCode());
            permissions.add(permission);
            if (menu.getChildren() != null) {
                this.builderPermissions(menu.getChildren(), permissions, role);
            }
        }
    }


    @Override
    @Transactional
    public void deleteRolePermissionByMenuId(Long menuId) {
        sysRolePermissionMapper.deleteByMenuId(menuId);
    }

    @Override
    @Transactional
    public void syncRolePermissionPermissionCode(SysRolePermission sysRolePermissionDTO) {
        sysRolePermissionMapper.updatePermissionCodeByMenuId(sysRolePermissionDTO.getPermissionCode(), sysRolePermissionDTO.getMenuId());
    }

    @Override
    public List<SysRolePermission> loadPermissionsByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysRolePermission> query = Wrappers.lambdaQuery();
        query.eq(SysRolePermission::getRoleCode, roleCode);
        return sysRolePermissionMapper.selectList(query);
    }

}
