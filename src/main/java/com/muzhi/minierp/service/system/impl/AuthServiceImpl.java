package com.muzhi.minierp.service.system.impl;

import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.security.JwtTokenProvider;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.system.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * {@code AuthServiceImpl}: AuthServiceImpl
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/1/20 10:25
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final JwtTokenProvider jwtTokenProvider;

    private final AuthenticationManager authenticationManager;

    @Override
    public LoginUser login(LoginQuery query) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(query.getUsername(), query.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        LoginUser user = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.createToken(user);
        user.setAccessToken(token);
        return user;
    }

    @Override
    public LoginUser getCurrentUser() {
        LoginUser currentUser = SecurityUtils.getCurrentUser();
        Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
        List<String> permissionCodes = new ArrayList<>(16);
        for (GrantedAuthority authority : authorities) {
            permissionCodes.add(authority.getAuthority());
        }
        currentUser.setPermissionCodes(permissionCodes);
        return currentUser;
    }
}
