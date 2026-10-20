package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultKeySlot;
import com.muzhi.minierp.mapper.vault.VaultKeySlotMapper;
import com.muzhi.minierp.service.vault.IVaultKeySlotService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * {@code VaultKeySlotServiceImpl}：密码库密钥槽数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultKeySlotServiceImpl extends ServiceImpl<VaultKeySlotMapper, VaultKeySlot> implements IVaultKeySlotService {

    @Override
    public List<VaultKeySlot> listByGeneration(Long vaultId, Long generation) {
        LambdaQueryWrapper<VaultKeySlot> query = Wrappers.lambdaQuery();
        query.eq(VaultKeySlot::getVaultId, vaultId);
        query.eq(VaultKeySlot::getGeneration, generation);
        query.orderByAsc(VaultKeySlot::getSlotType);
        return super.baseMapper.selectList(query);
    }
}
