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

    Long lockLoginUser(@Param("username") String username);

    int ensureAccount(@Param("ownerId") Long ownerId, @Param("id") Long id, @Param("handle") byte[] handle);

    VaultAccount lockAccount(@Param("ownerId") Long ownerId);

    VaultAccount activeAccount(@Param("ownerId") Long ownerId);

    VaultInfo lockVault(@Param("accountId") Long accountId);

    VaultRotation lockRotation(@Param("vaultId") Long vaultId, @Param("rotationId") UUID rotationId);

    Long ownerOfAccount(@Param("accountId") Long accountId);

    Long ownerOfVault(@Param("vaultId") Long vaultId);

    VaultGeneration generation(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    Long maxGeneration(@Param("vaultId") Long vaultId);

    OffsetDateTime databaseNow();

    VaultIdempotencyRecord receipt(@Param("accountId") Long accountId, @Param("operation") String operation, @Param("key") UUID key);

    List<VaultSessionRevocationOutbox> claimRevocations();

    List<VaultRotation> expiredRotations();

    int purgeDeletedItems(@Param("before") OffsetDateTime before);

    int purgeDeletedSlots(@Param("before") OffsetDateTime before);

    int purgeGenerationItems(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    int purgeGenerationSlots(@Param("vaultId") Long vaultId, @Param("generation") Long generation);
}
