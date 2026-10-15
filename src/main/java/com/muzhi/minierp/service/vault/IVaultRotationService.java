package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultRotation;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultRotationService extends IService<VaultRotation> {

}
