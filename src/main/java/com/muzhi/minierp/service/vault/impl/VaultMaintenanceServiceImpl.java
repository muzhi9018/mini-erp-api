package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.muzhi.minierp.config.VaultProperties;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.service.vault.IVaultMaintenanceService;
import com.muzhi.minierp.component.vault.VaultAccess;
import com.muzhi.minierp.model.vault.VaultProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * <p>
 * {@code VaultMaintenanceServiceImpl}：工作队列短事务与受控密文清理
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultMaintenanceServiceImpl implements IVaultMaintenanceService {

    private final VaultControlMapper control;

    private final VaultSessionRevocationOutboxMapper outbox;

    private final VaultGenerationMapper generations;

    private final VaultAccess access;

    private final VaultItemMapper items;

    private final VaultInfoMapper vaults;

    private final VaultRotationMapper rotations;

    private final PlatformTransactionManager transactionManager;

    private final VaultProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<VaultSessionRevocationOutbox> claim() {
        List<VaultSessionRevocationOutbox> jobs = control.claimRevocations();
        OffsetDateTime now = control.databaseNow();
        for (VaultSessionRevocationOutbox job : jobs) {
            job.setStatus(VaultEnum.OutboxStatus.PROCESSING.getCode());
            job.setLeaseUntil(now.plusSeconds(30));
            job.setAttempts(job.getAttempts() + 1);
            job.setGmtModified(now);
            outbox.updateById(job);
        }
        return jobs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(VaultSessionRevocationOutbox job, boolean success) {
        OffsetDateTime now = control.databaseNow();
        LambdaUpdateWrapper<VaultSessionRevocationOutbox> update = Wrappers.lambdaUpdate();
        update.eq(VaultSessionRevocationOutbox::getId, job.getId());
        update.eq(VaultSessionRevocationOutbox::getStatus, VaultEnum.OutboxStatus.PROCESSING.getCode());
        update.eq(VaultSessionRevocationOutbox::getLeaseUntil, job.getLeaseUntil());
        update.set(VaultSessionRevocationOutbox::getStatus, success ? VaultEnum.OutboxStatus.DONE.getCode() : VaultEnum.OutboxStatus.PENDING.getCode());
        update.set(VaultSessionRevocationOutbox::getLeaseUntil, null);
        update.set(VaultSessionRevocationOutbox::getGmtModified, now);
        if (success) {
            update.set(VaultSessionRevocationOutbox::getCompletedAt, now);
            update.set(VaultSessionRevocationOutbox::getLastErrorCode, null);
        } else {
            long delay = Math.min(300, 1L << Math.min(job.getAttempts(), 8));
            update.set(VaultSessionRevocationOutbox::getAvailableAt, now.plusSeconds(delay));
            update.set(VaultSessionRevocationOutbox::getLastErrorCode, VaultEnum.Error.UNAVAILABLE.getMachineCode());
        }
        outbox.update(null, update);
    }

    @Override
    public void purge() {
        OffsetDateTime now = control.databaseNow();
        OffsetDateTime itemBefore = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        OffsetDateTime before = now.minus(properties.getPurgeDelay());
        List<Long> owners = control.cleanupOwners(itemBefore, before);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        for (Long owner : owners) {
            try {
                // 每库独立短事务，单库失败不回滚其他库，也不长期持有多库锁。
                transaction.executeWithoutResult(status -> this.purgeVault(owner));
            } catch (RuntimeException failure) {
                log.warn("密码库密文清理失败，将在下一轮重试");
            }
        }
        OffsetDateTime oldest = control.oldestExpiredDeletion(itemBefore);
        if (oldest != null) {
            OffsetDateTime oldestPurgeAt = oldest.plusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
            log.warn("密码库到期密文仍待清理，最早到期时间：{}", oldestPurgeAt);
        }
    }

    private void purgeVault(Long owner) {
        VaultAccount account = control.lockCleanupAccount(owner);
        if (account == null) {
            return;
        }
        VaultInfo vault = control.lockCleanupVault(account.getId());
        if (vault == null) {
            return;
        }
        LambdaQueryWrapper<VaultRotation> open = Wrappers.lambdaQuery();
        open.eq(VaultRotation::getVaultId, vault.getId());
        open.eq(VaultRotation::getStatus, VaultEnum.RotationStatus.UPLOADING.getCode());
        boolean frozen = rotations.exists(open);
        if (frozen) {
            return;
        }
        OffsetDateTime now = control.databaseNow();
        OffsetDateTime itemBefore = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        OffsetDateTime before = now.minus(properties.getPurgeDelay());
        int batchSize = 100;
        int changed = items.purgeExpired(vault.getId(), itemBefore, batchSize);
        if (changed > 0) {
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
            vault.setCollectionRevision(vault.getCollectionRevision() + 1);
            vault.setGmtModified(now);
            vault.setUpdateUser(null);
            vaults.updateById(vault);
            access.systemAudit(account.getId(), vault.getId(), VaultEnum.AuditAction.ITEM_PURGE);
        }
        control.purgeDeletedSlots(vault.getId(), before);
        LambdaQueryWrapper<VaultGeneration> query = Wrappers.lambdaQuery();
        query.eq(VaultGeneration::getVaultId, vault.getId());
        query.in(VaultGeneration::getStatus, VaultEnum.GenerationStatus.RETIRED.getCode(), VaultEnum.GenerationStatus.ABORTED.getCode());
        query.le(VaultGeneration::getRetiredAt, before);
        query.orderByAsc(VaultGeneration::getRetiredAt);
        query.last("LIMIT 20");
        List<VaultGeneration> candidates = generations.selectList(query);
        for (VaultGeneration generation : candidates) {
            if (generation.getGeneration().equals(vault.getActiveGeneration())) {
                continue;
            }
            control.purgeGenerationItems(vault.getId(), generation.getGeneration());
            control.purgeGenerationSlots(vault.getId(), generation.getGeneration());
            LambdaUpdateWrapper<VaultGeneration> update = Wrappers.lambdaUpdate();
            update.eq(VaultGeneration::getId, generation.getId());
            update.set(VaultGeneration::getIsDeleted, true);
            update.set(VaultGeneration::getGmtDeleted, now);
            update.set(VaultGeneration::getGmtModified, now);
            generations.update(null, update);
        }
    }
}
