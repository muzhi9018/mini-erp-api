package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultPasskey;

/**
 * <p>
 * WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultPasskeyMapper extends BaseMapper<VaultPasskey> {

}
