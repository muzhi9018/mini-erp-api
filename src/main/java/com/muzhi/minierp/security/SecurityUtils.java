package com.muzhi.minierp.security;

import com.muzhi.minierp.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Security context helper methods.
 */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }


    /**
     * 密码加盐
     * @author Mr.Muzhi
     * @since 2026/4/23 18:21
     * @param username 用户名
     * @param password 密码
     * @return 加盐的密码
     */
    public static String passwordSaltAddition(String username, String password) {
        String content = username + "@" + password;
        return  DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }
}
