package com.muzhi.minierp.security;

import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.entity.system.SysRolePermission;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.enums.RedisKey;
import com.muzhi.minierp.enums.SysUserStatus;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.model.Role;
import com.muzhi.minierp.model.User;
import com.muzhi.minierp.service.system.ISysRoleService;
import com.muzhi.minierp.service.system.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final ISysUserService sysUserService;

    private final ISysRoleService sysRoleService;

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, User> redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        LoginUser user = this.loadLoginUser(username, password);
        if (user == null || StringUtils.isBlank(password) || StringUtils.isBlank(user.getPassword())) {
            throw new BadCredentialsException("system.auth.invalid-credentials");
        }
        String presentedPassword = SecurityUtils.passwordSaltAddition(username, password);
        if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
            throw new BadCredentialsException("system.auth.invalid-credentials");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("system.auth.account-locked");
        }
        RedisKey.User kye = RedisKey.User.USERINFO;
        User userinfo = new User();
        userinfo.setId(user.getId());
        userinfo.setUsername(username);
        userinfo.setUserNo(user.getUserNo());
        userinfo.setRealName(user.getRealName());
        userinfo.setPermissionCodes(user.getPermissionCodes());
        userinfo.setRoleCode(user.getRoleCode());
        userinfo.setRoles(user.getRoles());
        redisTemplate.opsForValue().set(kye.getKey(user.getUsername()), userinfo, kye.getTimeout());
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private LoginUser loadLoginUser(String username, String password) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            return null;
        }
        String roleCode = user.getCurrentRoleCode();
        List<SysRolePermission> permissions = sysRoleService.loadPermissionsByRoleCode(roleCode);
        List<SysRole> sysRoles = sysRoleService.findRoleByBindUserId(user.getId());
        List<Role> roles = new ArrayList<>(16);
        sysRoles.forEach(sysRole -> {
            Role role = new Role();
            role.setId(sysRole.getId());
            role.setRoleName(sysRole.getRoleName());
            role.setRoleCode(sysRole.getRoleCode());
            roles.add(role);
        });
        List<String> permissionCodes = permissions
                .stream()
                .map(SysRolePermission::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(permissionCodes);
        LoginUser loginUser = new LoginUser(user.getUsername(), user.getPassword(), SysUserStatus.isEnabled(user.getStatus()), authorities);
        loginUser.setId(user.getId());
        loginUser.setUserNo(user.getUserNo());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCode(roleCode);
        loginUser.setTokenType("Bearer");
        loginUser.setRoles(roles);
        loginUser.setPermissionCodes(permissionCodes);
        return loginUser;
    }
}
