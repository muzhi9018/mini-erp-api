package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultRotationBatch;

/**
 * <p>
 * 暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultRotationBatchMapper extends BaseMapper<VaultRotationBatch> {

}
