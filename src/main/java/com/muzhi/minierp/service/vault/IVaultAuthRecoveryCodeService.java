package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAuthRecoveryCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultAuthRecoveryCodeService extends IService<VaultAuthRecoveryCode> {

}
