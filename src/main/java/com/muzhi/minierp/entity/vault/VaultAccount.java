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
 * 密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_account")
public class VaultAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
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
    @TableField("active_recovery_batch_id")
    private Object activeRecoveryBatchId;

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
