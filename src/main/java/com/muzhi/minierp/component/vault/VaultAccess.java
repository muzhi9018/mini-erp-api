package com.muzhi.minierp.component.vault;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.service.vault.IVaultAccountService;
import com.muzhi.minierp.service.vault.IVaultAuditEventService;
import com.muzhi.minierp.service.vault.IVaultControlService;
import com.muzhi.minierp.service.vault.IVaultSessionRevocationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/**
 * <p>
 * {@code VaultAccess}：密码库事务内的归属与认证代次检查、审计和撤销记录
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultAccess {

    private final IVaultControlService vaultControlService;

    private final IVaultAccountService vaultAccountService;

    private final IVaultSessionRevocationOutboxService vaultSessionRevocationOutboxService;

    private final IVaultAuditEventService vaultAuditEventService;

    public VaultAccount check(VaultSession session) {
        VaultAccount account = vaultControlService.activeAccount(session.ownerId());
        VaultEnum.Error.SESSION_REVOKED.rejectIf(account == null || Boolean.TRUE.equals(account.getIsDeleted()) || !account.getId().equals(session.accountId())
                || account.getAuthEpoch() != session.authEpoch());
        return account;
    }

    /**
     * 写操作必须在事务内先锁定认证账户，再访问库、任务或条目。
     */
    public VaultAccount lock(VaultSession session) {
        VaultAccount account = vaultControlService.lockAccount(session.ownerId());
        VaultEnum.Error.SESSION_REVOKED.rejectIf(account == null || Boolean.TRUE.equals(account.getIsDeleted()) || !account.getId().equals(session.accountId())
                || account.getAuthEpoch() != session.authEpoch());
        VaultAccount active = vaultControlService.activeAccount(session.ownerId());
        VaultEnum.Error.SESSION_REVOKED.rejectIf(active == null);
        return account;
    }

    public VaultInfo lockVault(VaultSession session) {
        VaultInfo vault = vaultControlService.lockVault(session.accountId());
        VaultEnum.Error.NOT_FOUND.rejectIf(vault == null);
        return vault;
    }

    public void dataAccess(VaultAccount account) {
        VaultEnum.Error.RECOVERY_SETUP_REQUIRED.rejectIf(Boolean.TRUE.equals(account.getRecoveryRequired()));
    }

    public void create(VaultAuditEntity entity, Long actor) {
        OffsetDateTime now = OffsetDateTime.now();
        if (entity.getGmtCreate() == null) {
            entity.setGmtCreate(now);
        }
        entity.setGmtModified(now);
        entity.setCreateUser(actor);
        entity.setUpdateUser(actor);
        entity.setIsDeleted(false);
        entity.setDeleteUser(null);
        entity.setGmtDeleted(null);
    }

    public void touch(VaultAuditEntity entity, Long actor) {
        entity.setGmtModified(OffsetDateTime.now());
        entity.setUpdateUser(actor);
    }

    public void audit(VaultSession session, Long vaultId, VaultEnum.AuditAction action) {
        this.audit(session.accountId(), session.ownerId(), vaultId, action);
    }

    public void systemAudit(Long accountId, Long vaultId, VaultEnum.AuditAction action) {
        this.audit(accountId, null, vaultId, action);
    }

    private void audit(Long accountId, Long actor, Long vaultId, VaultEnum.AuditAction action) {
        VaultAuditEvent event = new VaultAuditEvent();
        event.setId(IdWorker.getId());
        event.setAccountId(accountId);
        event.setVaultId(vaultId);
        event.setAction(action.getCode());
        event.setResult(VaultEnum.AuditResult.SUCCESS.getCode());
        event.setRequestId(com.muzhi.minierp.security.vault.VaultContexts.auditRequestId());
        this.create(event, actor);
        vaultAuditEventService.save(event);
    }

    /**
     * 同事务递增认证代次并持久化清理任务；Redis 不参与数据库提交。
     */
    public long revoke(VaultAccount account, Long actor) {
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(account.getAuthEpoch() >= com.muzhi.minierp.model.vault.VaultProtocol.MAX_VERSION);
        account.setAuthEpoch(account.getAuthEpoch() + 1);
        this.touch(account, actor);
        vaultAccountService.updateById(account);
        VaultSessionRevocationOutbox job = new VaultSessionRevocationOutbox();
        job.setId(IdWorker.getId());
        job.setAccountId(account.getId());
        job.setAuthEpoch(account.getAuthEpoch());
        job.setStatus(VaultEnum.OutboxStatus.PENDING.getCode());
        job.setAttempts(0);
        job.setAvailableAt(OffsetDateTime.now());
        this.create(job, actor);
        vaultSessionRevocationOutboxService.save(job);
        return account.getAuthEpoch();
    }
}
