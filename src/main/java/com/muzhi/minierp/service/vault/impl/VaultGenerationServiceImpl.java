package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultGeneration;
import com.muzhi.minierp.mapper.vault.VaultGenerationMapper;
import com.muzhi.minierp.service.vault.IVaultGenerationService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code VaultGenerationServiceImpl}：密码库代次数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultGenerationServiceImpl extends ServiceImpl<VaultGenerationMapper, VaultGeneration> implements IVaultGenerationService {
}
