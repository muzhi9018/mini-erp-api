package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultGeneration;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 密码库代次控制记录；清理密文后保留最小记录供轮换回执引用 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultGenerationService extends IService<VaultGeneration> {

}
