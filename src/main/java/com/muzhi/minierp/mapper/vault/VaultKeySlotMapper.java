package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultKeySlot;

/**
 * <p>
 * 每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultKeySlotMapper extends BaseMapper<VaultKeySlot> {

}
