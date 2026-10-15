package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultAuditEvent;
import com.muzhi.minierp.mapper.vault.VaultAuditEventMapper;
import com.muzhi.minierp.service.vault.IVaultAuditEventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultAuditEventServiceImpl extends ServiceImpl<VaultAuditEventMapper, VaultAuditEvent> implements IVaultAuditEventService {

}
