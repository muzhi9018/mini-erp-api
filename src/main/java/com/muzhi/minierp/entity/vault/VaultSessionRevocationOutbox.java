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
 * 数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_session_revocation_outbox")
public class VaultSessionRevocationOutbox implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 待清理会话的认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 清理会话代次小于该值的会话，不能删除之后新建的会话
     */
    @TableField("auth_epoch")
    private Long authEpoch;

    /**
     * PENDING=待重试，PROCESSING=持有租约，DONE=已完成
     */
    @TableField("status")
    private String status;

    /**
     * 处理次数
     */
    @TableField("attempts")
    private Integer attempts;

    /**
     * 下一次可领取时间，失败按退避策略重试
     */
    @TableField("available_at")
    private LocalDateTime availableAt;

    /**
     * 工作线程租约截止，崩溃后可重新领取
     */
    @TableField("lease_until")
    private LocalDateTime leaseUntil;

    /**
     * 服务端固定错误码，不记录异常正文或 Session ID
     */
    @TableField("last_error_code")
    private String lastErrorCode;

    /**
     * 完成清理时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

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
