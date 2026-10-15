package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultKeySlot;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultKeySlotService extends IService<VaultKeySlot> {

}
