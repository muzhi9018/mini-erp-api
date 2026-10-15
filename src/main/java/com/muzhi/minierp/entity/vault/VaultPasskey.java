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
 * WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_passkey")
public class VaultPasskey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
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
    @TableField("transports")
    private Object transports;

    /**
     * 认证器型号标识，由已验证的注册结果取得
     */
    @TableField("aaguid")
    private Object aaguid;

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
    private LocalDateTime lastUsedAt;

    /**
     * 撤销时间；被撤销的凭据不得用于认证
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
