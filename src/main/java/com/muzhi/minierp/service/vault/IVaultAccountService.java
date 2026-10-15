package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultAccountService extends IService<VaultAccount> {

}
