package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.mapper.vault.VaultAccountMapper;
import com.muzhi.minierp.service.vault.IVaultAccountService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code VaultAccountServiceImpl}：密码库认证账户数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultAccountServiceImpl extends ServiceImpl<VaultAccountMapper, VaultAccount> implements IVaultAccountService {
}
