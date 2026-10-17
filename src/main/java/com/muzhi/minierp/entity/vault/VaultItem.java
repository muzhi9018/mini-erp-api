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
 * {@code VaultItem}：整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_item", schema = "public", autoResultMap = true)
public class VaultItem extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = -1746567259695197300L;

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
     * 所属代次；普通查询只能读取活动代
     */
    @TableField("generation")
    private Long generation;

    /**
     * 密钥代次，由 Service 在事务内校验与所属代一致
     */
    @TableField("key_epoch")
    private Long keyEpoch;

    /**
     * 公开条目 ID，库内跨轮换保持稳定
     */
    @TableField(value = "item_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID itemId;

    /**
     * 用于 AAD 的内容版本；普通编辑递增，完整轮换保持原值
     */
    @TableField("content_version")
    private Long contentVersion;

    /**
     * 服务端行版本；普通编辑递增，新代重新从 1 开始
     */
    @TableField("row_version")
    private Long rowVersion;

    /**
     * 条目密钥密文盒版本
     */
    @TableField("wrapped_dek_format_version")
    private Integer wrappedDekFormatVersion;

    /**
     * 条目密钥认证加密算法
     */
    @TableField("wrapped_dek_algorithm")
    private String wrappedDekAlgorithm;

    /**
     * 包装 DEK 使用的独立随机 IV
     */
    @TableField("wrapped_dek_iv")
    private byte[] wrappedDekIv;

    /**
     * 32 字节 DEK 密文及 16 字节认证标签
     */
    @TableField("wrapped_dek_ciphertext")
    private byte[] wrappedDekCiphertext;

    /**
     * 条目内容密文盒版本
     */
    @TableField("encrypted_item_format_version")
    private Integer encryptedItemFormatVersion;

    /**
     * 条目内容认证加密算法
     */
    @TableField("encrypted_item_algorithm")
    private String encryptedItemAlgorithm;

    /**
     * 加密条目使用的独立随机 IV
     */
    @TableField("encrypted_item_iv")
    private byte[] encryptedItemIv;

    /**
     * 最多 16 KiB 明文对应的密文与 16 字节认证标签
     */
    @TableField("encrypted_item_ciphertext")
    private byte[] encryptedItemCiphertext;
}
