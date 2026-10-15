package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultSessionRevocationOutbox;

/**
 * <p>
 * 数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultSessionRevocationOutboxMapper extends BaseMapper<VaultSessionRevocationOutbox> {

}
