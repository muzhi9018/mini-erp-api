package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultAccount}：密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_account", schema = "public", autoResultMap = true)
public class VaultAccount extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 现有不可变系统用户 ID；API ownerSubject 为其规范十进制字符串
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 随机 WebAuthn 用户句柄，固定 32 字节，不使用用户名或邮箱
     */
    @TableField("webauthn_user_handle")
    private byte[] webauthnUserHandle;

    /**
     * 认证代次；请求必须与会话代次相等，敏感变更在数据库事务内递增
     */
    @TableField("auth_epoch")
    private Long authEpoch;

    /**
     * 凭据集合版本，用于新增、撤销和恢复操作的并发检查
     */
    @TableField("credential_revision")
    private Long credentialRevision;

    /**
     * 首次绑定或身份恢复后，未确认新一批 MFA 恢复码时禁止数据接口
     */
    @TableField("recovery_required")
    private Boolean recoveryRequired;

    /**
     * 当前有效 MFA 恢复码批次；未确认的批次不能用于恢复
     */
    @TableField(value = "active_recovery_batch_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID activeRecoveryBatchId;
}
