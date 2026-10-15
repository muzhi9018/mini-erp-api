package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultRotationExpectedItem;
import com.muzhi.minierp.mapper.vault.VaultRotationExpectedItemMapper;
import com.muzhi.minierp.service.vault.IVaultRotationExpectedItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultRotationExpectedItemServiceImpl extends ServiceImpl<VaultRotationExpectedItemMapper, VaultRotationExpectedItem> implements IVaultRotationExpectedItemService {

}
