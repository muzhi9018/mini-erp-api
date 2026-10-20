package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultItem;
import com.muzhi.minierp.mapper.vault.VaultItemMapper;
import com.muzhi.minierp.service.vault.IVaultItemService;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * {@code VaultItemServiceImpl}：密码库密文条目数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultItemServiceImpl extends ServiceImpl<VaultItemMapper, VaultItem> implements IVaultItemService {

    @Override
    public List<VaultItem> listByGeneration(Long vaultId, Long generation) {
        LambdaQueryWrapper<VaultItem> query = Wrappers.lambdaQuery();
        query.eq(VaultItem::getVaultId, vaultId);
        query.eq(VaultItem::getGeneration, generation);
        query.orderByAsc(VaultItem::getItemId);
        return super.baseMapper.selectList(query);
    }

    @Override
    public Cursor<VaultItem> snapshotItems(Long vaultId, Long generation) {
        return super.baseMapper.snapshotItems(vaultId, generation);
    }
}
