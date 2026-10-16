package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

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
}
