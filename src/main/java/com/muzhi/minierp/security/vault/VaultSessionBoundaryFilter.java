package com.muzhi.minierp.security.vault;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * <p>
 * {@code VaultSessionBoundaryFilter}：统一 Redis 会话加载和保存阶段的错误响应
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@RequiredArgsConstructor
public class VaultSessionBoundaryFilter extends OncePerRequestFilter {

    private final VaultErrorWriter errors;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !VaultPaths.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException | ServletException failure) {
            if (response.isCommitted()) {
                throw failure;
            }
            response.resetBuffer();
            errors.writeException(request, response, failure);
        }
    }
}
