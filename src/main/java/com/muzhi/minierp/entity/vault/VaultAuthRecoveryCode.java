package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultAuthRecoveryCode}：仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_auth_recovery_code", schema = "public", autoResultMap = true)
public class VaultAuthRecoveryCode extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = 2177338620261053262L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 恢复码批次；只有账户 active_recovery_batch_id 指向的批次有效
     */
    @TableField(value = "batch_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID batchId;

    /**
     * 独立随机 32 字节恢复码的 SHA-256 摘要；明文仅生成时返回一次
     */
    @TableField("code_digest")
    private byte[] codeDigest;

    /**
     * 该批次经用户重新导入校验后的确认时间
     */
    @TableField("confirmed_at")
    private OffsetDateTime confirmedAt;

    /**
     * 单次原子消费时间
     */
    @TableField("consumed_at")
    private OffsetDateTime consumedAt;

    /**
     * 替换批次或安全操作后的撤销时间
     */
    @TableField("revoked_at")
    private OffsetDateTime revokedAt;
}
