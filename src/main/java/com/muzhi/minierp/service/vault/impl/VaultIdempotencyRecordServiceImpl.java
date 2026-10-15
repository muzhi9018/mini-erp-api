package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.muzhi.minierp.mapper.vault.VaultIdempotencyRecordMapper;
import com.muzhi.minierp.service.vault.IVaultIdempotencyRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultIdempotencyRecordServiceImpl extends ServiceImpl<VaultIdempotencyRecordMapper, VaultIdempotencyRecord> implements IVaultIdempotencyRecordService {

}
