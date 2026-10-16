package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultPasskey}：WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_passkey", schema = "public", autoResultMap = true)
public class VaultPasskey extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属密码库认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * WebAuthn 凭据 ID；HTTP 中使用无填充 Base64url
     */
    @TableField("credential_id")
    private byte[] credentialId;

    /**
     * 验证库输出的 COSE 公钥
     */
    @TableField("public_key_cose")
    private byte[] publicKeyCose;

    /**
     * 签名计数，由 WebAuthn 库按认证器类型执行检查，不自行要求每次必增
     */
    @TableField("signature_count")
    private Long signatureCount;

    /**
     * 认证器传输提示；不作为认证成功依据
     */
    @TableField(value = "transports", typeHandler = VaultArrayTypeHandler.class)
    private String[] transports;

    /**
     * 认证器型号标识，由已验证的注册结果取得
     */
    @TableField(value = "aaguid", typeHandler = VaultUuidTypeHandler.class)
    private UUID aaguid;

    /**
     * 凭据是否允许同步或备份
     */
    @TableField("backup_eligible")
    private Boolean backupEligible;

    /**
     * 凭据是否已备份；不能在不支持备份时为真
     */
    @TableField("backup_state")
    private Boolean backupState;

    /**
     * 用户可见名称，仅作为纯文本展示
     */
    @TableField("label")
    private String label;

    /**
     * 最近一次成功认证时间
     */
    @TableField("last_used_at")
    private OffsetDateTime lastUsedAt;

    /**
     * 撤销时间；被撤销的凭据不得用于认证
     */
    @TableField("revoked_at")
    private OffsetDateTime revokedAt;
}
