package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultAuditEvent;
import com.muzhi.minierp.mapper.vault.VaultAuditEventMapper;
import com.muzhi.minierp.service.vault.IVaultAuditEventService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code VaultAuditEventServiceImpl}：密码库审计事件数据服务实现
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
@Service
public class VaultAuditEventServiceImpl extends ServiceImpl<VaultAuditEventMapper, VaultAuditEvent> implements IVaultAuditEventService {
}
