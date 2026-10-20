package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultSessionRevocationOutbox;

import java.util.List;

/**
 * <p>
 * {@code IVaultMaintenanceService}：会话撤销任务维护与受控密文清理
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
public interface IVaultMaintenanceService {

    /**
     * 在短事务中领取待撤销会话任务并设置处理租约。
     *
     * @return 本次领取的撤销任务
     */
    List<VaultSessionRevocationOutbox> claim();

    /**
     * 按领取时的租约确认处理结果，失败任务延迟重试。
     *
     * @param job 已领取的撤销任务
     * @param success 是否处理成功
     */
    void finish(VaultSessionRevocationOutbox job, boolean success);

    /**
     * 按保留策略清理已删除的密文及退役、废弃代数据。
     */
    void purge();
}
