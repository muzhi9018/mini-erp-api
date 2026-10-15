package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultRotationExpectedItem;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultRotationExpectedItemService extends IService<VaultRotationExpectedItem> {

}
