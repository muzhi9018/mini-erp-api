package com.muzhi.minierp.security.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * <p>
 * {@code VaultBrowserSession}：认证提升后更新 Session ID、显式保存身份并轮换 CSRF token
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultBrowserSession {
    private final HttpSessionSecurityContextRepository contexts;

    private final HttpSessionCsrfTokenRepository csrf;

    public Map<String, Object> save(VaultSession identity, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        String authority = "VAULT_" + identity.stage().getCode();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(identity, null,
                List.of(new SimpleGrantedAuthority(authority)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        csrf.saveToken(null, request, response);
        return this.view(identity, request, response);
    }

    public Map<String, Object> view(VaultSession identity, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        CsrfToken token = csrf.loadToken(request);
        if (token == null) {
            token = csrf.generateToken(request);
            csrf.saveToken(token, request, response);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loginMethod", "LOCAL_PASSKEY");
        result.put("csrfToken", token.getToken());
        result.put("csrfHeader", token.getHeaderName());
        if (identity != null) {
            result.put("ownerSubject", identity.ownerId().toString());
            result.put("stage", identity.stage().getCode());
            result.put("mfa", identity.stage() == VaultEnum.Stage.MFA);
            result.put("recentAuthentication", identity.mfaAt() != null && identity.mfaAt().isAfter(java.time.Instant.now().minusSeconds(300)));
        }
        return result;
    }

    public void clear(HttpServletRequest request, HttpServletResponse response, boolean committedRevocation) {
        SecurityContextHolder.clearContext();
        response.addHeader("Set-Cookie", "__Host-vault_session=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax");
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (DataAccessException failure) {
                if (!committedRevocation) {
                    throw failure;
                }
                // 数据库已经撤销旧代，Redis 清理由持久化 outbox 重试。
            }
        }
    }
}
