package com.muzhi.minierp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzhi.minierp.dto.SysRolePermissionDTO;
import com.muzhi.minierp.entity.SysMenu;
import com.muzhi.minierp.mapper.SysMenuMapper;
import com.muzhi.minierp.mapper.SysRolePermissionMapper;
import com.muzhi.minierp.service.ISysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 系统菜单 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public IPage<SysMenu> findTopLevelMenu(Integer pageNum, Integer pageSize) {
        IPage<SysMenu> page = new Page<>(pageNum, pageSize);
        return baseMapper.findTopLevelMenu(page);
    }

    @Override
    public List<SysMenu> findByParentId(Long parentId) {
        return baseMapper.findByParentId(parentId);
    }

    @Override
    @Transactional
    public void addMenu(SysMenu sysMenu) {
        sysMenu.setId(IdWorker.getId());
        // 判断是否为顶级菜单
        if (sysMenu.getParentId().equals(0L)) {
            SysMenu parentMenu = baseMapper.findByClientIdAndParentId(sysMenu.getClientId(), sysMenu.getParentId());
            Assert.isNotNull(parentMenu, String.format("客户端[%s]顶级菜单已存在", sysMenu.getClientId()));
            sysMenu.setHierarchy(String.valueOf(sysMenu.getId()));
        } else {
            SysMenu parentMenu = baseMapper.selectById(sysMenu.getParentId());
            Assert.isNull(parentMenu, "父级菜单不存在");
            sysMenu.setHierarchy(parentMenu.getHierarchy() + "," + sysMenu.getId());
            sysMenu.setClientId(parentMenu.getClientId());
        }
        baseMapper.insert(sysMenu);
    }

    @Override
    @Transactional
    public void updateMenu(SysMenu sysMenu) {
        SysMenu dbMenu = baseMapper.selectById(sysMenu.getId());
        Assert.isNull(dbMenu, "菜单不存在");
        // 父级id、所属客户端、层级 不能修改
        sysMenu.setParentId(null);
        sysMenu.setClientId(null);
        sysMenu.setHierarchy(null);
        baseMapper.updateById(sysMenu);

        // 判断是否修改了权限码，修改了要同步到角色权限
        if (sysMenu.getPermissionCode() != null && !sysMenu.getPermissionCode().equals(dbMenu.getPermissionCode())) {
            SysRolePermissionDTO sysRolePermissionDTO = new SysRolePermissionDTO();
            sysRolePermissionDTO.setMenuId(dbMenu.getId());
            sysRolePermissionDTO.setPermissionCode(sysMenu.getPermissionCode());
            sysRolePermissionMapper.updatePermissionCodeByMenuId(sysRolePermissionDTO.getPermissionCode(), sysRolePermissionDTO.getMenuId());
        }
    }

    @Override
    @Transactional
    public void delById(Long id) {
        List<SysMenu> children = baseMapper.findByParentId(id);
        Assert.isNotEmpty(children, "当前菜单存在子级菜单,不能删除");
        sysRolePermissionMapper.deleteByMenuId(id);
        baseMapper.delById(id);
    }

    @Override
    public List<SysMenu> findByIds(List<Long> ids) {
        return baseMapper.findByIds(ids);
    }

}
