package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultIdempotencyRecordService extends IService<VaultIdempotencyRecord> {

}
