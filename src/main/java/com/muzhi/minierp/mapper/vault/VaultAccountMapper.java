package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultAccount;

/**
 * <p>
 * 密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultAccountMapper extends BaseMapper<VaultAccount> {

}
