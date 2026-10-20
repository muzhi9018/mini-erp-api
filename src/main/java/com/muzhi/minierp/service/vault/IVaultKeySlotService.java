package com.muzhi.minierp.service.vault;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.vault.VaultKeySlot;

import java.util.List;

/**
 * <p>
 * {@code IVaultKeySlotService}：密码库密钥槽数据服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
public interface IVaultKeySlotService extends IService<VaultKeySlot> {

    /**
     * 按密钥槽类型顺序查询指定代次的有效记录。
     *
     * @param vaultId 密码库内部 ID
     * @param generation 代次
     * @return 指定代次的有效记录
     */
    List<VaultKeySlot> listByGeneration(Long vaultId, Long generation);
}
