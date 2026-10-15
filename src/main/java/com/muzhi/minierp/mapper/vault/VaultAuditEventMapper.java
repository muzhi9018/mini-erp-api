package com.muzhi.minierp.mapper.vault;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.vault.VaultAuditEvent;

/**
 * <p>
 * 认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultAuditEventMapper extends BaseMapper<VaultAuditEvent> {

}
