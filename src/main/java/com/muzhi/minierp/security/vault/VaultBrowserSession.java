package com.muzhi.minierp.security.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import jakarta.servlet.http.*;
import com.muzhi.minierp.vo.vault.*;
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

    public VaultSessionVO save(VaultSession identity, HttpServletRequest request, HttpServletResponse response) {
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

    public VaultSessionVO view(VaultSession identity, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        CsrfToken token = csrf.loadToken(request);
        if (token == null) {
            token = csrf.generateToken(request);
            csrf.saveToken(token, request, response);
        }
        VaultSessionVO result = new VaultSessionVO();
        result.setLoginMethod("LOCAL_PASSKEY");
        result.setCsrfToken(token.getToken());
        result.setCsrfHeader(token.getHeaderName());
        if (identity != null) {
            result.setOwnerSubject(identity.ownerId().toString());
            result.setStage(identity.stage().getCode());
            result.setMfa(identity.stage() == VaultEnum.Stage.MFA);
            boolean recentAuthentication = identity.mfaAt() != null
                    && identity.mfaAt().isAfter(java.time.Instant.now().minusSeconds(300));
            result.setRecentAuthentication(recentAuthentication);
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
