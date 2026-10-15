package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultGeneration;

/**
 * <p>
 * 密码库代次控制记录；清理密文后保留最小记录供轮换回执引用 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultGenerationMapper extends BaseMapper<VaultGeneration> {

}
