package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.mapper.vault.VaultAccountMapper;
import com.muzhi.minierp.service.vault.IVaultAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultAccountServiceImpl extends ServiceImpl<VaultAccountMapper, VaultAccount> implements IVaultAccountService {

}
