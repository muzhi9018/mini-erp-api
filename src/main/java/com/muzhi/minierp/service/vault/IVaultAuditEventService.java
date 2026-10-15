package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAuditEvent;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultAuditEventService extends IService<VaultAuditEvent> {

}
