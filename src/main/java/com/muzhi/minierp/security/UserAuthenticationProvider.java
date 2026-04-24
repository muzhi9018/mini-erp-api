package com.muzhi.minierp.security;

import com.muzhi.minierp.entity.SysUser;
import com.muzhi.minierp.enums.SysUserStatus;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final ISysUserService sysUserService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        LoginUser loginUser = this.buildLoginUser(username, password);
        return new UsernamePasswordAuthenticationToken(
            loginUser,
            null,
            loginUser.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private LoginUser buildLoginUser(String username, String password) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null || !StringUtils.hasText(password) || !StringUtils.hasText(user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        String presentedPassword = SecurityUtils.passwordSaltAddition(username, password);
        if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (!SysUserStatus.isEnabled(user.getStatus())) {
            throw new DisabledException("用户已被禁用");
        }

        String roleCode = user.getCurrentRoleCode();
        String authority = StringUtils.hasText(roleCode) && roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + (StringUtils.hasText(roleCode) ? roleCode : "USER");

        LoginUser loginUser = new LoginUser(
            user.getUsername(),
            user.getPassword(),
            true,
            Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
        loginUser.setId(user.getId());
        loginUser.setUserNo(user.getUserNo());
        loginUser.setRoleCode(roleCode);
        return loginUser;
    }
}
