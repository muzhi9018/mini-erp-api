package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultSessionRevocationOutbox}：数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_session_revocation_outbox", schema = "public", autoResultMap = true)
public class VaultSessionRevocationOutbox extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
     * {@link com.muzhi.minierp.enums.VaultEnum.OutboxStatus}
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
    private OffsetDateTime availableAt;

    /**
     * 工作线程租约截止，崩溃后可重新领取
     */
    @TableField("lease_until")
    private OffsetDateTime leaseUntil;

    /**
     * 服务端固定错误码，不记录异常正文或 Session ID
     */
    @TableField("last_error_code")
    private String lastErrorCode;

    /**
     * 完成清理时间
     */
    @TableField("completed_at")
    private OffsetDateTime completedAt;
}
