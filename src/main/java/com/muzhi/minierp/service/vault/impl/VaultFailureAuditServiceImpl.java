package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.muzhi.minierp.entity.vault.VaultAuditEvent;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.VaultAuditEventMapper;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.security.vault.VaultContexts;
import com.muzhi.minierp.service.vault.IVaultFailureAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultFailureAuditServiceImpl}：失败审计独立提交，只保存固定动作和错误码
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultFailureAuditServiceImpl implements IVaultFailureAuditService {

    private final VaultAuditEventMapper audits;

    private final PlatformTransactionManager transactionManager;

    @Override
    public void record(HttpServletRequest request, VaultEnum.Error error) {
        String marker = IVaultFailureAuditService.class.getName();
        if (request.getAttribute(marker) != null) {
            return;
        }
        request.setAttribute(marker, true);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        VaultSession identity = authentication != null && authentication.getPrincipal() instanceof VaultSession session ? session : null;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        VaultEnum.AuditAction action = switch (path) {
            case "/vault/auth/login" -> VaultEnum.AuditAction.LOGIN;
            case "/vault/auth/passkeys/register/verify" -> VaultEnum.AuditAction.PASSKEY_REGISTER;
            case "/vault/auth/passkeys/authenticate/verify" -> VaultEnum.AuditAction.PASSKEY_AUTHENTICATE;
            case "/vault/auth/mfa-recovery/verify" -> VaultEnum.AuditAction.MFA_RECOVERY;
            default -> VaultEnum.AuditAction.SESSION_REJECT;
        };
        VaultAuditEvent event = new VaultAuditEvent();
        event.setId(IdWorker.getId());
        event.setAccountId(identity == null ? null : identity.accountId());
        event.setAction(action.getCode());
        event.setResult(error.getStatus() >= 500 ? VaultEnum.AuditResult.FAILURE.getCode() : VaultEnum.AuditResult.DENIED.getCode());
        event.setErrorCode(error.getMachineCode());
        event.setRequestId(UUID.fromString(VaultContexts.requestId(request)));
        OffsetDateTime now = OffsetDateTime.now();
        event.setGmtCreate(now);
        event.setGmtModified(now);
        event.setCreateUser(identity == null ? null : identity.ownerId());
        event.setIsDeleted(false);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaction.executeWithoutResult(status -> audits.insert(event));
        } catch (RuntimeException failure) {
            // 数据库故障时仍返回原错误，不输出请求内容或数据库异常正文。
            log.warn("密码库失败审计暂不可用");
        }
    }
}
