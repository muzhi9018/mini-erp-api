package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultRotation;

/**
 * <p>
 * 完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultRotationMapper extends BaseMapper<VaultRotation> {

}
