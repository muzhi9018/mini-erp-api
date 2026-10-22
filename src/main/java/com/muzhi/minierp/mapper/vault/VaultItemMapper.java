package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultItemMapper extends BaseMapper<VaultItem> {

    /**
     * 在事务内使用 PostgreSQL 游标逐条读取当前代的有效密文。
     */
    Cursor<VaultItem> snapshotItems(
            @Param("vaultId") Long vaultId,
            @Param("generation") Long generation
    );
    /**
     * 显式包含已删除行；调用前先锁认证账户和密码库。
     */
    VaultItem lockItem(@Param("vaultId") Long vaultId, @Param("generation") Long generation, @Param("itemId") UUID itemId);

    /**
     * 按删除时间倒序及条目标识读取首屏时刻的未到期集合。
     */
    List<VaultItem> deletedItems(@Param("vaultId") Long vaultId, @Param("generation") Long generation,
            @Param("before") OffsetDateTime before, @Param("firstTime") OffsetDateTime firstTime,
            @Param("lastDeleted") OffsetDateTime lastDeleted, @Param("lastId") UUID lastId, @Param("limit") int limit);

    OffsetDateTime earliestDeletedAt(@Param("vaultId") Long vaultId, @Param("generation") Long generation,
            @Param("before") OffsetDateTime before);

    /**
     * 轮换起点和空库判断使用正常及未到期已删除集合。
     */
    long retainedCount(@Param("vaultId") Long vaultId, @Param("generation") Long generation,
            @Param("before") OffsetDateTime before);

    /** 在数据库内冻结源 ID 和版本，不加载整库密文。 */
    int freezeSource(@Param("vaultId") Long vaultId, @Param("generation") Long generation,
            @Param("rotationId") Long rotationId, @Param("before") OffsetDateTime before,
            @Param("now") OffsetDateTime now, @Param("actor") Long actor);

    /**
     * 按任务冻结的 ID 集合读取，不因轮换期间自然到期而减少记录。
     */
    List<VaultItem> frozenItems(@Param("vaultId") Long vaultId, @Param("generation") Long generation,
            @Param("rotationId") Long rotationId, @Param("lastId") UUID lastId, @Param("limit") Integer limit,
            @Param("metadataOnly") boolean metadataOnly);

    /**
     * 仅内部验证使用，包含已删除的暂存行。
     */
    List<VaultItem> generationMetadata(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    /** 在事务内流式读取完整暂存代，包含已删除行。 */
    Cursor<VaultItem> generationItems(@Param("vaultId") Long vaultId, @Param("generation") Long generation);

    int markDeleted(@Param("item") VaultItem item, @Param("actor") Long actor, @Param("now") OffsetDateTime now);

    int restoreItem(@Param("item") VaultItem item, @Param("actor") Long actor, @Param("now") OffsetDateTime now,
            @Param("before") OffsetDateTime before);

    int purgeItem(@Param("item") VaultItem item);

    /** 按预期版本物理删除正常记录，供账号组直接删除使用。 */
    int deleteActiveItem(@Param("item") VaultItem item);

    /**
     * 手动永久删除一并移除同一条目的历史代副本。
     */
    int purgeHistoricalCopies(@Param("vaultId") Long vaultId, @Param("itemId") UUID itemId);

    /**
     * 持库锁后重新校验期限；limit 为空时供轮换终态清理全部到期行。
     */
    int purgeExpired(@Param("vaultId") Long vaultId, @Param("before") OffsetDateTime before, @Param("limit") Integer limit);
}
