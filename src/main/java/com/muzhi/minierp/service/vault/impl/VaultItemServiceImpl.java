package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultItem;
import com.muzhi.minierp.mapper.vault.VaultItemMapper;
import com.muzhi.minierp.service.vault.IVaultItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultItemServiceImpl extends ServiceImpl<VaultItemMapper, VaultItem> implements IVaultItemService {

}
