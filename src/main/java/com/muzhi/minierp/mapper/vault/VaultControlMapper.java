package com.muzhi.minierp.mapper.vault;

import com.muzhi.minierp.entity.vault.*;
import org.apache.ibatis.annotations.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * {@code VaultControlMapper}：归属、行锁、历史回执与工作队列访问
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultControlMapper {

    /** 判断密码库表是否已迁移，供模块关闭时的用户删除保护使用。 */
    boolean accountTableExists();

    Long lockLoginUser(@Param("username") String username);

    int ensureAccount(@Param("ownerId") Long ownerId, @Param("id") Long id, @Param("handle") byte[] handle);

    VaultAccount lockAccount(@Param("ownerId") Long ownerId);

    VaultAccount activeAccount(@Param("ownerId") Long ownerId);

    VaultInfo lockVault(@Param("accountId") Long accountId);

    VaultRotation lockRotation(@Param("vaultId") Long vaultId, @Param("rotationId") UUID rotationId);

    Long ownerOfAccount(@Param("accountId") Long accountId);

    Long ownerOfVault(@Param("vaultId") Long vaultId);

    VaultGeneration generation(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    /** 检查活动代内的标识占用，包含尚未清理的逻辑删除条目。 */
    boolean itemIdExists(@Param("vaultId") Long vaultId, @Param("generation") Long generation, @Param("itemId") UUID itemId);

    Long maxGeneration(@Param("vaultId") Long vaultId);

    OffsetDateTime databaseNow();

    VaultIdempotencyRecord receipt(@Param("accountId") Long accountId, @Param("operation") String operation, @Param("key") UUID key);

    List<VaultSessionRevocationOutbox> claimRevocations();

    List<VaultRotation> expiredRotations();

    /** 逐库清理候选，领取时仍须按账户、库顺序跳过已锁行。 */
    List<Long> cleanupOwners(@Param("itemBefore") OffsetDateTime itemBefore, @Param("before") OffsetDateTime before);

    VaultAccount lockCleanupAccount(@Param("ownerId") Long ownerId);

    VaultInfo lockCleanupVault(@Param("accountId") Long accountId);

    OffsetDateTime oldestExpiredDeletion(@Param("before") OffsetDateTime before);

    int purgeDeletedSlots(@Param("vaultId") Long vaultId, @Param("before") OffsetDateTime before);

    int purgeGenerationItems(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    int purgeGenerationSlots(@Param("vaultId") Long vaultId, @Param("generation") Long generation);
}
