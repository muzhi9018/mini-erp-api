package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultRotation;
import com.muzhi.minierp.mapper.vault.VaultRotationMapper;
import com.muzhi.minierp.service.vault.IVaultRotationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultRotationServiceImpl extends ServiceImpl<VaultRotationMapper, VaultRotation> implements IVaultRotationService {

}
