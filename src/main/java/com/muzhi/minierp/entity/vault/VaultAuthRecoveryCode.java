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
 * 仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_auth_recovery_code")
public class VaultAuthRecoveryCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 所属认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 恢复码批次；只有账户 active_recovery_batch_id 指向的批次有效
     */
    @TableField("batch_id")
    private Object batchId;

    /**
     * 独立随机 32 字节恢复码的 SHA-256 摘要；明文仅生成时返回一次
     */
    @TableField("code_digest")
    private byte[] codeDigest;

    /**
     * 该批次经用户重新导入校验后的确认时间
     */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 单次原子消费时间
     */
    @TableField("consumed_at")
    private LocalDateTime consumedAt;

    /**
     * 替换批次或安全操作后的撤销时间
     */
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

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
