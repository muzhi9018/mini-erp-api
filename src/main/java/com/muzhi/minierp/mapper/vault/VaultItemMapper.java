package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultItem;

/**
 * <p>
 * 整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultItemMapper extends BaseMapper<VaultItem> {

}
