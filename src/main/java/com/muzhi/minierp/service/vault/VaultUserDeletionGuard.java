package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.mapper.vault.VaultControlMapper;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * {@code VaultUserDeletionGuard}：阻止删除仍保留密码库数据的系统用户，模块关闭时仍生效
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-17
 */
@Component
@RequiredArgsConstructor
public class VaultUserDeletionGuard {

    private final VaultControlMapper control;

    /**
     * 在删除事务持有系统用户行锁后调用；保留策略未完成前不能遗留孤立密文或认证记录。
     *
     * @param ownerId 待删除的系统用户 ID
     */
    public void requireNoRetainedData(Long ownerId) {
        boolean migrated = control.accountTableExists();
        if (!migrated) {
            return;
        }
        // 包括逻辑删除的账户；软删除不能代表关联数据已按保留策略处理完毕。
        VaultAccount account = control.lockAccount(ownerId);
        Assert.isTrue(account != null, "system.user.vault-data-retained", "用户仍有密码库数据，请先完成数据处置；如需停用账号请禁用用户");
    }
}
