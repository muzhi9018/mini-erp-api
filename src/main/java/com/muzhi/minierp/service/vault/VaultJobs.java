package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.mapper.vault.VaultControlMapper;
import com.muzhi.minierp.model.vault.VaultSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * <p>
 * {@code VaultJobs}：Redis 会话撤销重试与超时任务回收；不输出令牌或异常正文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultJobs {
    private final VaultMaintenanceService maintenance;

    private final VaultControlMapper control;

    private final IVaultRotationService rotations;

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    @Scheduled(fixedDelay = 15000)
    public void revokeSessions() {
        try {
            List<VaultSessionRevocationOutbox> jobs = maintenance.claim();
            for (VaultSessionRevocationOutbox job : jobs) {
                boolean success;
                try {
                    this.revoke(job);
                    success = true;
                } catch (RuntimeException failure) {
                    success = false;
                }
                maintenance.finish(job, success);
            }
        } catch (RuntimeException failure) {
            log.warn("密码库会话撤销任务暂不可用，将重试");
        }
    }

    public void revoke(VaultSessionRevocationOutbox job) {
        Long owner = control.ownerOfAccount(job.getAccountId());
        if (owner == null) {
            return;
        }
        Map<String, ? extends Session> found = sessions.findByPrincipalName(owner.toString());
        for (Session session : found.values()) {
            SecurityContext context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context != null && context.getAuthentication() != null
                    && context.getAuthentication().getPrincipal() instanceof VaultSession identity
                    && identity.accountId().equals(job.getAccountId()) && identity.authEpoch() < job.getAuthEpoch()) {
                sessions.deleteById(session.getId());
            }
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void expireRotations() {
        try {
            List<VaultRotation> expired = control.expiredRotations();
            for (VaultRotation task : expired) {
                Long owner = control.ownerOfVault(task.getVaultId());
                if (owner != null) {
                    rotations.expire(owner, task.getRotationId().toString());
                }
            }
        } catch (RuntimeException failure) {
            log.warn("密码库过期任务回收暂不可用，将重试");
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void purgeCiphertext() {
        try {
            maintenance.purge();
        } catch (RuntimeException failure) {
            log.warn("密码库旧密文清理暂不可用，将重试");
        }
    }
}
