package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultSessionRevocationOutbox;
import com.muzhi.minierp.mapper.vault.VaultSessionRevocationOutboxMapper;
import com.muzhi.minierp.service.vault.IVaultSessionRevocationOutboxService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultSessionRevocationOutboxServiceImpl extends ServiceImpl<VaultSessionRevocationOutboxMapper, VaultSessionRevocationOutbox> implements IVaultSessionRevocationOutboxService {

}
