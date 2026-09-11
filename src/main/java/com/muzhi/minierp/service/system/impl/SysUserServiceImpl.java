package com.muzhi.minierp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.enums.SysUserStatus;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.system.SysUserMapper;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.system.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Override
    public SysUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> query = Wrappers.lambdaQuery();
        query.eq(SysUser::getUsername, username);
        return super.baseMapper.selectOne(query);
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
        super.baseMapper.insert(user);
    }
}
