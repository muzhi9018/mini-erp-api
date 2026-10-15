package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultPasskey;
import com.muzhi.minierp.mapper.vault.VaultPasskeyMapper;
import com.muzhi.minierp.service.vault.IVaultPasskeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultPasskeyServiceImpl extends ServiceImpl<VaultPasskeyMapper, VaultPasskey> implements IVaultPasskeyService {

}
