package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultKeySlot;
import com.muzhi.minierp.mapper.vault.VaultKeySlotMapper;
import com.muzhi.minierp.service.vault.IVaultKeySlotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultKeySlotServiceImpl extends ServiceImpl<VaultKeySlotMapper, VaultKeySlot> implements IVaultKeySlotService {

}
