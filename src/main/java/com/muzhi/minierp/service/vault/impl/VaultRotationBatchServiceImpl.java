package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultRotationBatch;
import com.muzhi.minierp.mapper.vault.VaultRotationBatchMapper;
import com.muzhi.minierp.service.vault.IVaultRotationBatchService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultRotationBatchServiceImpl extends ServiceImpl<VaultRotationBatchMapper, VaultRotationBatch> implements IVaultRotationBatchService {

}
