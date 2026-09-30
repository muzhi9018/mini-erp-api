package com.muzhi.minierp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.entity.system.SysUserRole;
import com.muzhi.minierp.enums.SysUserStatus;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.system.SysRoleMapper;
import com.muzhi.minierp.mapper.system.SysUserMapper;
import com.muzhi.minierp.mapper.system.SysUserRoleMapper;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.system.ISysUserService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 系统用户信息 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-04-23
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final PasswordEncoder passwordEncoder;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public SysUser findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getUsername, username);
        return baseMapper.selectOne(query);
    }

    @Override
    public void create(SysUser user) {
        SysUser dbUser = this.findByUsername(user.getUsername());
        if (dbUser != null) {
            throw new BusinessException("system.user.username-already-exists", "用户名已存在");
        }
        String password = SecurityUtils.passwordSaltAddition(user.getUsername(), user.getPassword());
        password = passwordEncoder.encode(password);
        user.setPassword(password);
        if (user.getStatus() == null) {
            user.setStatus(SysUserStatus.ENABLED.value());
        }
        baseMapper.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeRole(Long userId, Long roleId) {
        SysUser user = baseMapper.selectById(userId);
        Assert.isNull(user, "system.user.not-found", "用户不存在");

        SysRole role = sysRoleMapper.selectById(roleId);
        Assert.isNull(role, "system.role.not-found", "角色不存在");

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        userRoleQuery.eq(SysUserRole::getRoleId, roleId);
        boolean alreadyAuthorized = sysUserRoleMapper.exists(userRoleQuery);
        Assert.isTrue(alreadyAuthorized, "system.user-role.already-exists", "用户已拥有该角色");

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        sysUserRoleMapper.insert(userRole);
        if (StringUtils.isBlank(user.getCurrentRoleCode())) {
            user.setCurrentRoleCode(role.getRoleCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        Assert.isNull(user, "system.user.not-found", "用户不存在");

        int minimumEightDigitNumber = 10_000_000;
        int firstNineDigitNumber = 100_000_000;
        SecureRandom secureRandom = new SecureRandom();
        String generatedPassword = Integer.toString(secureRandom.nextInt(minimumEightDigitNumber, firstNineDigitNumber));
        String saltedPassword = SecurityUtils.passwordSaltAddition(user.getUsername(), generatedPassword);
        String encodedPassword = passwordEncoder.encode(saltedPassword);
        SysUser passwordUpdate = new SysUser();
        passwordUpdate.setId(userId);
        passwordUpdate.setPassword(encodedPassword);
        int updated = baseMapper.updateById(passwordUpdate);
        Assert.isTrue(updated != 1, "system.user.reset-password.save-failed", "重置密码失败");
        return generatedPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        Assert.isNull(user, "system.user.not-found", "用户不存在");

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(userRoleQuery);

        int deleted = baseMapper.deleteById(userId);
        Assert.isTrue(deleted != 1, "system.user.not-found", "用户不存在");
    }

    @Override
    public IPage<SysUserVO> findByPage(Integer pageNum, Integer pageSize, SysUser query) {
        IPage<SysUserVO> result = new Page<>(pageNum, pageSize);
        result = super.baseMapper.findByPage(result, query);
        List<SysUserVO> users = result.getRecords();
        if (users.isEmpty()) {
            return result;
        }

        Set<Long> userIds = new HashSet<>();
        for (SysUserVO user : users) {
            userIds.add(user.getId());
            user.setRoles(new ArrayList<>());
        }

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.in(SysUserRole::getUserId, userIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoles.isEmpty()) {
            return result;
        }

        Set<Long> roleIds = new HashSet<>();
        for (SysUserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);
        Map<Long, SysRole> rolesById = new HashMap<>();
        for (SysRole role : roles) {
            rolesById.put(role.getId(), role);
        }

        Map<Long, SysUserVO> usersById = new HashMap<>();
        for (SysUserVO user : users) {
            usersById.put(user.getId(), user);
        }
        for (SysUserRole userRole : userRoles) {
            SysRole role = rolesById.get(userRole.getRoleId());
            if (role != null) {
                usersById.get(userRole.getUserId()).getRoles().add(role);
            }
        }
        for (SysUserVO user : users) {
            user.getRoles().sort(Comparator.comparing(SysRole::getSort, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(SysRole::getId));
        }
        return result;
    }
}
