package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultRotationBatch;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultRotationBatchService extends IService<VaultRotationBatch> {

}
