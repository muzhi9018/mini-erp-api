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
 * {@code VaultKeySlot}：每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_key_slot", schema = "public", autoResultMap = true)
public class VaultKeySlot extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = 332723563434212902L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 内部密码库逻辑 ID
     */
    @TableField("vault_id")
    private Long vaultId;

    /**
     * 所属代次
     */
    @TableField("generation")
    private Long generation;

    /**
     * 公开密钥槽 ID；每次重建槽生成新值
     */
    @TableField(value = "slot_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID slotId;

    /**
     * NORMAL=主密码加账户密钥，RECOVERY=独立恢复密钥
     * {@link com.muzhi.minierp.enums.VaultEnum.SlotType}
     */
    @TableField("slot_type")
    private String slotType;

    /**
     * NORMAL 对应 accountKeyId，RECOVERY 对应 recoveryKeyId；不是秘密本身
     */
    @TableField(value = "key_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID keyId;

    /**
     * 密文盒版本，v1 仅支持 1
     */
    @TableField("format_version")
    private Integer formatVersion;

    /**
     * 认证加密算法，v1 固定 A256GCM
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 每次加密独立生成的 12 字节 IV
     */
    @TableField("iv")
    private byte[] iv;

    /**
     * 32 字节 VMK 密文及末尾 16 字节 GCM 标签
     */
    @TableField("ciphertext")
    private byte[] ciphertext;

    /**
     * 正常槽固定 KDF 档案，不接收任意内存或迭代参数
     */
    @TableField("kdf_profile")
    private String kdfProfile;

    /**
     * 正常槽公开 Argon2 盐
     */
    @TableField("kdf_salt")
    private byte[] kdfSalt;

    /**
     * 正常槽公开 HKDF 盐
     */
    @TableField("wrap_salt")
    private byte[] wrapSalt;

    /**
     * 恢复槽公开 HKDF 盐
     */
    @TableField("recovery_salt")
    private byte[] recoverySalt;
}
