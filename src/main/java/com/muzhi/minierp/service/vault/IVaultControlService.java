package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.entity.vault.VaultGeneration;
import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.entity.vault.VaultRotation;
import com.muzhi.minierp.enums.VaultEnum;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * {@code IVaultControlService}：密码库归属、行锁和控制数据查询
 * 加锁方法参与调用方事务，不单独创建或结束事务。
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
public interface IVaultControlService {

    /**
     * 判断认证账户表是否已迁移，供模块关闭时的用户删除保护使用。
     */
    boolean accountTableExists();

    /**
     * 查询用户当前有效的认证账户。
     *
     * @param ownerId 系统用户 ID
     * @return 有效认证账户，不存在时返回 null
     */
    VaultAccount activeAccount(Long ownerId);

    /**
     * 在调用方事务中锁定用户的认证账户，包含逻辑删除记录。
     *
     * @param ownerId 系统用户 ID
     * @return 认证账户，不存在时返回 null
     */
    VaultAccount lockAccount(Long ownerId);

    /**
     * 在调用方事务中锁定认证账户所属的密码库。
     *
     * @param accountId 认证账户 ID
     * @return 密码库，不存在时返回 null
     */
    VaultInfo lockVault(Long accountId);

    /**
     * 查询认证账户所属的系统用户 ID。
     *
     * @param accountId 认证账户 ID
     * @return 系统用户 ID，不存在时返回 null
     */
    Long ownerOfAccount(Long accountId);

    /**
     * 查询密码库所属的系统用户 ID。
     *
     * @param vaultId 密码库内部 ID
     * @return 系统用户 ID，不存在时返回 null
     */
    Long ownerOfVault(Long vaultId);

    /**
     * 查询指定代次的控制记录。
     *
     * @param vaultId 密码库内部 ID
     * @param generation 代次
     * @return 代次控制记录，不存在时返回 null
     */
    VaultGeneration generation(Long vaultId, Long generation);

    /**
     * 查询密码库已使用的最大代次，包含已清理代次。
     *
     * @param vaultId 密码库内部 ID
     * @return 最大代次，无记录时返回 0
     */
    Long maxGeneration(Long vaultId);

    /**
     * 查询认证账户指定操作的持久化幂等回执。
     *
     * @param accountId 认证账户 ID
     * @param operation 操作类型
     * @param key 幂等键
     * @return 幂等回执，不存在时返回 null
     */
    VaultIdempotencyRecord receipt(Long accountId, VaultEnum.Operation operation, UUID key);

    /**
     * 按数据库时间查询本轮待回收的超时轮换任务。
     *
     * @return 超时轮换任务
     */
    List<VaultRotation> expiredRotations();
}
