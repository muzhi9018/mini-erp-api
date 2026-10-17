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
 * {@code VaultIdempotencyRecord}：业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_idempotency_record", schema = "public", autoResultMap = true)
public class VaultIdempotencyRecord extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = -4130824562395353926L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 业务操作类型；批次幂等使用 rotation_batch
     * {@link com.muzhi.minierp.enums.VaultEnum.Operation}
     */
    @TableField("operation")
    private String operation;

    /**
     * 客户端随机幂等键
     */
    @TableField(value = "idempotency_key", typeHandler = VaultUuidTypeHandler.class)
    private UUID idempotencyKey;

    /**
     * 绑定账户、操作及规范业务参数的 SHA-256 摘要
     */
    @TableField("request_digest")
    private byte[] requestDigest;

    /**
     * 已成功提交结果的真实 HTTP 状态
     */
    @TableField("http_status")
    private Integer httpStatus;

    /**
     * 仅保存最小公开 JSON 回执；禁止保存任何解密秘密、MFA 恢复码或会话令牌
     */
    @TableField(value = "response_body", typeHandler = VaultJsonTypeHandler.class)
    private String responseBody;
}
