package com.muzhi.minierp.service.vault;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.vault.VaultItem;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;

/**
 * <p>
 * {@code IVaultItemService}：密码库密文条目数据服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
public interface IVaultItemService extends IService<VaultItem> {

    /**
     * 按公开标识顺序查询指定代次的有效记录。
     *
     * @param vaultId 密码库内部 ID
     * @param generation 代次
     * @return 指定代次的有效记录
     */
    List<VaultItem> listByGeneration(Long vaultId, Long generation);

    /**
     * 在调用方持有账户与库行锁的事务中，以游标读取指定代次的有效密文。
     * 调用方必须在同一事务内遍历并关闭游标。
     *
     * @param vaultId 密码库内部 ID
     * @param generation 代次
     * @return 由调用方关闭的密文游标
     */
    Cursor<VaultItem> snapshotItems(Long vaultId, Long generation);
}
