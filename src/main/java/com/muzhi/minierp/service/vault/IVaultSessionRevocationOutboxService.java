package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultSessionRevocationOutbox;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultSessionRevocationOutboxService extends IService<VaultSessionRevocationOutbox> {

}
