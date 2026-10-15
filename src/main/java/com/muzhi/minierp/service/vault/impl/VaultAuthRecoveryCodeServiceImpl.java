package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultAuthRecoveryCode;
import com.muzhi.minierp.mapper.vault.VaultAuthRecoveryCodeMapper;
import com.muzhi.minierp.service.vault.IVaultAuthRecoveryCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultAuthRecoveryCodeServiceImpl extends ServiceImpl<VaultAuthRecoveryCodeMapper, VaultAuthRecoveryCode> implements IVaultAuthRecoveryCodeService {

}
