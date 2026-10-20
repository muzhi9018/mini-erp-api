package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.muzhi.minierp.mapper.vault.VaultIdempotencyRecordMapper;
import com.muzhi.minierp.service.vault.IVaultIdempotencyRecordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code VaultIdempotencyRecordServiceImpl}：密码库幂等回执数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultIdempotencyRecordServiceImpl extends ServiceImpl<VaultIdempotencyRecordMapper, VaultIdempotencyRecord> implements IVaultIdempotencyRecordService {
}
