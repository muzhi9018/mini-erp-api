package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;

/**
 * <p>
 * 业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultIdempotencyRecordMapper extends BaseMapper<VaultIdempotencyRecord> {

}
