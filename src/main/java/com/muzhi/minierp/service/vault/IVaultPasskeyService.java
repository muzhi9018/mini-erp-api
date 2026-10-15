package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultPasskey;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultPasskeyService extends IService<VaultPasskey> {

}
