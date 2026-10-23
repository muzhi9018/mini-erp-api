package com.muzhi.minierp.security.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import java.time.Instant;
import java.util.UUID;

/**
 * <p>
 * {@code VaultContexts}：可信会话访问与近期认证判断
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public final class VaultContexts {
    public static final String MFA_AUTHORITY = "VAULT_MFA";

    private VaultContexts() { }

    public static String requestId(jakarta.servlet.http.HttpServletRequest request) {
        String key = VaultContexts.class.getName() + ".requestId";
        Object existing = request.getAttribute(key);
        if (existing instanceof String id) {
            return id;
        }
        String id = UUID.randomUUID().toString();
        request.setAttribute(key, id);
        return id;
    }

    public static UUID auditRequestId() {
        RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            return UUID.fromString(VaultContexts.requestId(servlet.getRequest()));
        }
        return UUID.randomUUID();
    }

    public static VaultSession current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof VaultSession session)) {
            throw VaultEnum.Error.SESSION_EXPIRED.exception();
        }
        return session;
    }

    public static void requireMfa(VaultSession session) {
        VaultEnum.Error.MFA_REQUIRED.rejectIf(session.stage() != VaultEnum.Stage.MFA);
    }

    public static void requireRecent(VaultSession session) {
        VaultContexts.requireMfa(session);
        VaultContexts.requireRecentPasskey(session);
    }

    public static void requireRecentPasskey(VaultSession session) {
        Instant now = Instant.now();
        VaultEnum.Error.REAUTH_REQUIRED.rejectIf(session.mfaAt() == null || session.mfaAt().isBefore(now.minusSeconds(300)));
    }
}
