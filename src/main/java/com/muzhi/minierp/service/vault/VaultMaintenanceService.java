package com.muzhi.minierp.service.vault;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.muzhi.minierp.config.VaultProperties;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * {@code VaultMaintenanceService}：工作队列短事务与受控密文清理
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultMaintenanceService {
    private final VaultControlMapper control;

    private final VaultSessionRevocationOutboxMapper outbox;

    private final VaultGenerationMapper generations;

    private final VaultCipherStore cipher;

    private final VaultProperties properties;

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

    @Transactional(rollbackFor = Exception.class)
    public void purge() {
        OffsetDateTime now = control.databaseNow();
        OffsetDateTime before = now.minus(properties.getPurgeDelay());
        control.purgeDeletedItems(before);
        control.purgeDeletedSlots(before);
        LambdaQueryWrapper<VaultGeneration> query = Wrappers.lambdaQuery();
        query.in(VaultGeneration::getStatus, VaultEnum.GenerationStatus.RETIRED.getCode(), VaultEnum.GenerationStatus.ABORTED.getCode());
        query.le(VaultGeneration::getRetiredAt, before);
        query.orderByAsc(VaultGeneration::getRetiredAt);
        query.last("LIMIT 20");
        List<VaultGeneration> candidates = generations.selectList(query);
        Map<Long, Long> owners = new HashMap<>();
        for (VaultGeneration candidate : candidates) {
            Long owner = control.ownerOfVault(candidate.getVaultId());
            if (owner != null) {
                owners.put(candidate.getId(), owner);
            }
        }
        candidates.sort(Comparator.comparing(candidate -> owners.getOrDefault(candidate.getId(), Long.MAX_VALUE)));
        for (VaultGeneration generation : candidates) {
            Long owner = owners.get(generation.getId());
            if (owner == null) {
                continue;
            }
            VaultAccount account = control.lockAccount(owner);
            VaultInfo vault = account == null ? null : control.lockVault(account.getId());
            if (vault == null || generation.getGeneration().equals(vault.getActiveGeneration())) {
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
