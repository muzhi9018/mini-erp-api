package com.muzhi.minierp.security.vault;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * {@code VaultPaths}：独立安全链的路径边界
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public final class VaultPaths {
    private VaultPaths() { }

    public static boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/vault/auth") || path.startsWith("/vault/auth/")
                || path.equals("/vault/api") || path.startsWith("/vault/api/");
    }
}
