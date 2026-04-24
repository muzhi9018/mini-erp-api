package com.muzhi.minierp.service.impl;

import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.security.JwtTokenProvider;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

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
        user.setToken(token);
        return user;
    }

    @Override
    public LoginUser getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }
}
