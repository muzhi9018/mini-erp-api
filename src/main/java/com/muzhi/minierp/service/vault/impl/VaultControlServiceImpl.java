package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.entity.vault.VaultGeneration;
import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.entity.vault.VaultRotation;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.VaultControlMapper;
import com.muzhi.minierp.service.vault.IVaultControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * {@code VaultControlServiceImpl}：密码库控制数据查询，不改变调用方事务边界
 * 模块关闭时仍提供用户删除保护所需的表迁移检查和账户行锁。
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
@RequiredArgsConstructor
public class VaultControlServiceImpl implements IVaultControlService {

    /**
     * 密码库控制数据 Mapper。
     */
    private final VaultControlMapper vaultControlMapper;

    @Override
    public boolean accountTableExists() {
        return vaultControlMapper.accountTableExists();
    }

    @Override
    public VaultAccount activeAccount(Long ownerId) {
        return vaultControlMapper.activeAccount(ownerId);
    }

    @Override
    public VaultAccount lockAccount(Long ownerId) {
        return vaultControlMapper.lockAccount(ownerId);
    }

    @Override
    public VaultInfo lockVault(Long accountId) {
        return vaultControlMapper.lockVault(accountId);
    }

    @Override
    public Long ownerOfAccount(Long accountId) {
        return vaultControlMapper.ownerOfAccount(accountId);
    }

    @Override
    public Long ownerOfVault(Long vaultId) {
        return vaultControlMapper.ownerOfVault(vaultId);
    }

    @Override
    public VaultGeneration generation(Long vaultId, Long generation) {
        return vaultControlMapper.generation(vaultId, generation);
    }

    @Override
    public Long maxGeneration(Long vaultId) {
        return vaultControlMapper.maxGeneration(vaultId);
    }

    @Override
    public VaultIdempotencyRecord receipt(Long accountId, VaultEnum.Operation operation, UUID key) {
        String operationCode = operation.getCode();
        return vaultControlMapper.receipt(accountId, operationCode, key);
    }

    @Override
    public List<VaultRotation> expiredRotations() {
        return vaultControlMapper.expiredRotations();
    }
}
