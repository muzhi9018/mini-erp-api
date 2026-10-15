package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultGeneration;
import com.muzhi.minierp.mapper.vault.VaultGenerationMapper;
import com.muzhi.minierp.service.vault.IVaultGenerationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 密码库代次控制记录；清理密文后保留最小记录供轮换回执引用 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultGenerationServiceImpl extends ServiceImpl<VaultGenerationMapper, VaultGeneration> implements IVaultGenerationService {

}
