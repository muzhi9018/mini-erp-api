package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultRotationExpectedItem;

/**
 * <p>
 * 轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultRotationExpectedItemMapper extends BaseMapper<VaultRotationExpectedItem> {

}
