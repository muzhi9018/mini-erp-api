package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultItem;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultItemService extends IService<VaultItem> {

}
