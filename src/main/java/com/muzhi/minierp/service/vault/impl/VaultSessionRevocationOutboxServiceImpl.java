package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultSessionRevocationOutbox;
import com.muzhi.minierp.mapper.vault.VaultSessionRevocationOutboxMapper;
import com.muzhi.minierp.service.vault.IVaultSessionRevocationOutboxService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code VaultSessionRevocationOutboxServiceImpl}：密码库会话撤销任务数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultSessionRevocationOutboxServiceImpl extends ServiceImpl<VaultSessionRevocationOutboxMapper, VaultSessionRevocationOutbox> implements IVaultSessionRevocationOutboxService {
}
