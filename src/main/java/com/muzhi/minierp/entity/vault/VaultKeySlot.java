package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_key_slot")
public class VaultKeySlot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
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
    @TableField("slot_id")
    private Object slotId;

    /**
     * NORMAL=主密码加账户密钥，RECOVERY=独立恢复密钥
     */
    @TableField("slot_type")
    private String slotType;

    /**
     * NORMAL 对应 accountKeyId，RECOVERY 对应 recoveryKeyId；不是秘密本身
     */
    @TableField("key_id")
    private Object keyId;

    /**
     * 密文盒版本，v1 仅支持 1
     */
    @TableField("format_version")
    private Short formatVersion;

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

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间，由应用更新，DEFAULT 不会自动更新
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 最近更新人系统用户 ID，由服务端填充；系统任务可为空
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 逻辑删除标记，false=有效、true=已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID
     */
    @TableField("delete_user")
    private Long deleteUser;

    /**
     * 逻辑删除时间，删除时与标记及删除人一起写入
     */
    @TableField("gmt_deleted")
    private LocalDateTime gmtDeleted;
}
