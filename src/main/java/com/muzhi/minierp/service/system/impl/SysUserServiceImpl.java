package com.muzhi.minierp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.entity.system.SysUserRole;
import com.muzhi.minierp.enums.DefaultRoleEnum;
import com.muzhi.minierp.enums.SysRoleEnum;
import com.muzhi.minierp.enums.SysUserStatus;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.system.SysRoleMapper;
import com.muzhi.minierp.mapper.system.SysUserMapper;
import com.muzhi.minierp.mapper.system.SysUserRoleMapper;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.system.ISysUserService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.service.vault.IVaultIdentityService;
import com.muzhi.minierp.component.vault.VaultUserDeletionGuard;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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

    private final ObjectProvider<IVaultIdentityService> vaultIdentities;

    private final VaultUserDeletionGuard vaultUserDeletionGuard;

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
    public void authorizeRole(Long userId, List<Long> roleIds) {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        Assert.isNull(loginUser, "http.status.unauthorized", "请先登录");
        Long operatorId = loginUser.getId();
        SysUser user = super.baseMapper.selectById(userId);
        Assert.isNull(user, "system.user.not-found", "用户不存在");

        // 授权权限以当前登录角色为准，不提升到账号拥有的最高权限角色。
        LambdaQueryWrapper<SysRole> currentRoleQuery = Wrappers.lambdaQuery();
        currentRoleQuery.eq(SysRole::getRoleCode, loginUser.getRoleCode());
        SysRole currentRole = sysRoleMapper.selectOne(currentRoleQuery);
        Assert.isNull(currentRole, "system.user-role.operation-forbidden", "无权操作，当前登录角色权限不足");

        LambdaQueryWrapper<SysUserRole> currentBindingQuery = Wrappers.lambdaQuery();
        currentBindingQuery.eq(SysUserRole::getUserId, operatorId);
        currentBindingQuery.eq(SysUserRole::getRoleId, currentRole.getId());
        boolean currentRoleBound = sysUserRoleMapper.exists(currentBindingQuery);
        Assert.isFalse(currentRoleBound, "system.user-role.operation-forbidden", "无权操作，当前登录角色权限不足");
        boolean operatingSelf = operatorId.equals(userId);

        LambdaQueryWrapper<SysUserRole> previousRoleQuery = Wrappers.lambdaQuery();
        previousRoleQuery.select(SysUserRole::getRoleId);
        previousRoleQuery.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> previousUserRoles = sysUserRoleMapper.selectList(previousRoleQuery);
        Set<Long> previousRoleIds = new HashSet<>();
        for (SysUserRole userRole : previousUserRoles) {
            previousRoleIds.add(userRole.getRoleId());
        }
        Long targetHighestRoleId = previousRoleIds.stream().min(Long::compareTo).orElse(null);
        boolean targetNotLower = !operatingSelf && targetHighestRoleId != null && currentRole.getId() >= targetHighestRoleId;
        Assert.isTrue(targetNotLower, "system.user-role.operation-forbidden", "无权操作，当前登录角色权限不足");

        Set<Long> retainedRoleIds = new HashSet<>();
        if (operatingSelf) {
            // 自身已拥有且权限不低于当前登录角色的角色必须保留，前端漏传时自动补齐。
            for (Long previousRoleId : previousRoleIds) {
                if (previousRoleId <= currentRole.getId()) {
                    retainedRoleIds.add(previousRoleId);
                }
            }
        }

        Set<Long> authorizedRoleIds = new TreeSet<>(roleIds);
        for (Long roleId : authorizedRoleIds) {
            boolean retainedRole = retainedRoleIds.contains(roleId);
            Assert.isTrue(!retainedRole && roleId == DefaultRoleEnum.SUPER_ADMIN.getId(), "system.user-role.super-admin-forbidden", "不能授予超级管理员角色");
            Assert.isTrue(!retainedRole && roleId <= currentRole.getId(), "system.user-role.role-level-forbidden", "不能授予高于或等于当前登录角色权限的角色");
        }
        authorizedRoleIds.addAll(retainedRoleIds);
        List<SysRole> authorizedRoles = new ArrayList<>();
        if (!authorizedRoleIds.isEmpty()) {
            authorizedRoles = sysRoleMapper.selectByIds(authorizedRoleIds);
            Assert.isTrue(authorizedRoles.size() != authorizedRoleIds.size(), "system.role.not-found", "角色不存在");
            for (SysRole role : authorizedRoles) {
                DefaultRoleEnum defaultRole = DefaultRoleEnum.ofCode(role.getRoleCode());
                boolean retainedRole = retainedRoleIds.contains(role.getId());
                Assert.isTrue(!retainedRole && defaultRole == DefaultRoleEnum.SUPER_ADMIN, "system.user-role.super-admin-forbidden", "不能授予超级管理员角色");
            }
        }

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        if (!authorizedRoleIds.isEmpty()) {
            userRoleQuery.notIn(SysUserRole::getRoleId, authorizedRoleIds);
        }
        sysUserRoleMapper.delete(userRoleQuery);

        for (Long roleId : authorizedRoleIds) {
            if (!previousRoleIds.contains(roleId)) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                int inserted = sysUserRoleMapper.insert(userRole);
                Assert.isTrue(inserted != 1, "system.user-role.save-failed", "用户角色授权失败，请重试");
            }
        }

        boolean currentRoleRetained = authorizedRoles.stream().anyMatch(role -> role.getRoleCode().equals(user.getCurrentRoleCode()));
        if (!currentRoleRetained) {
            SysRole nextRole = authorizedRoles.stream().min(Comparator.comparing(SysRole::getId)).orElse(null);
            String nextRoleCode = nextRole == null ? null : nextRole.getRoleCode();
            if (Objects.equals(nextRoleCode, user.getCurrentRoleCode())) {
                return;
            }
            // 显式设置空值，确保清空授权时一并清除当前角色编码。
            LambdaUpdateWrapper<SysUser> currentRoleUpdate = Wrappers.lambdaUpdate();
            currentRoleUpdate.eq(SysUser::getId, userId);
            currentRoleUpdate.set(SysUser::getCurrentRoleCode, nextRoleCode);
            SysUser updateUser = new SysUser();
            int updated = super.baseMapper.update(updateUser, currentRoleUpdate);
            Assert.isTrue(updated != 1, "system.user-role.save-failed", "用户角色授权失败，请重试");
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
        vaultIdentities.ifAvailable(service -> service.revokeOwner(userId));
        return generatedPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        // 与密码库登录的用户行共享锁互斥，避免检查后又创建密码库账户。
        LambdaQueryWrapper<SysUser> userQuery = Wrappers.lambdaQuery();
        userQuery.eq(SysUser::getId, userId);
        userQuery.last("FOR UPDATE");
        SysUser user = super.baseMapper.selectOne(userQuery);
        Assert.isNull(user, "system.user.not-found", "用户不存在");
        vaultUserDeletionGuard.requireNoRetainedData(userId);

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(userRoleQuery);

        int deleted = baseMapper.deleteById(userId);
        Assert.isTrue(deleted != 1, "system.user.not-found", "用户不存在");
        vaultIdentities.ifAvailable(service -> service.revokeOwner(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer changeStatus(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        Assert.isNull(user, "system.user.not-found", "用户不存在");

        Integer currentStatus = user.getStatus();
        SysUserStatus nextStatus;
        if (SysUserStatus.ENABLED.eq(currentStatus)) {
            nextStatus = SysUserStatus.DISABLED;
        } else if (SysUserStatus.DISABLED.eq(currentStatus)) {
            nextStatus = SysUserStatus.ENABLED;
        } else {
            throw new BusinessException("system.user.change-status.invalid-status", "用户状态无效，无法切换");
        }

        // 仅在状态未被其他请求修改时更新，避免并发切换互相覆盖。
        LambdaUpdateWrapper<SysUser> statusCondition = Wrappers.lambdaUpdate();
        statusCondition.eq(SysUser::getId, userId);
        statusCondition.eq(SysUser::getStatus, currentStatus);
        statusCondition.set(SysUser::getStatus, nextStatus.value());
        int updated = baseMapper.update(null, statusCondition);
        Assert.isTrue(updated != 1, "system.user.change-status.save-failed", "切换用户状态失败，请重试");
        vaultIdentities.ifAvailable(service -> service.revokeOwner(userId));
        return nextStatus.value();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeCurrentRole(String currentRoleCode) {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        // 锁定用户记录，串行校验并更新同一用户的当前角色。
        LambdaQueryWrapper<SysUser> userQuery = Wrappers.lambdaQuery();
        userQuery.eq(SysUser::getId, loginUser.getId());
        SysUser user = super.baseMapper.selectOne(userQuery);
        Assert.isNull(user, "system.user.not-found", "用户不存在");
        Assert.isTrue(currentRoleCode.equals(user.getCurrentRoleCode()), "system.user.change-current-role.already-current", "当前角色已经是该角色，无需切换");

        LambdaQueryWrapper<SysRole> roleQuery = Wrappers.lambdaQuery();
        roleQuery.eq(SysRole::getRoleCode, currentRoleCode);
        SysRole role = sysRoleMapper.selectOne(roleQuery);
        Assert.isNull(role, "system.role.not-found", "角色不存在");
        Assert.isFalse(SysRoleEnum.Status.ENABLED.eq(role.getStatus()), "system.user.change-current-role.role-unavailable", "角色状态异常，无法切换");

        LambdaQueryWrapper<SysUserRole> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRole::getUserId, user.getId());
        userRoleQuery.eq(SysUserRole::getRoleId, role.getId());
        boolean bound = sysUserRoleMapper.exists(userRoleQuery);
        Assert.isFalse(bound, "system.user.change-current-role.not-bound", "当前用户未绑定该角色，无法切换");

        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        updateUser.setCurrentRoleCode(role.getRoleCode());
        int updated = super.baseMapper.updateById(updateUser);
        Assert.isTrue(updated != 1, "system.user.change-current-role.save-failed", "切换角色失败，请重试");
    }

    @Override
    public IPage<SysUserVO> findByPage(Integer pageNum, Integer pageSize, SysUser query) {
        IPage<SysUserVO> result = new Page<>(pageNum, pageSize);
        result = baseMapper.findByPage(result, query);
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
