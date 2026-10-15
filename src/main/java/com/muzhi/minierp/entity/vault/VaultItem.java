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
 * 整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_item")
public class VaultItem implements Serializable {

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
    @TableField("item_id")
    private Object itemId;

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
    private Short wrappedDekFormatVersion;

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
    private Short encryptedItemFormatVersion;

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
