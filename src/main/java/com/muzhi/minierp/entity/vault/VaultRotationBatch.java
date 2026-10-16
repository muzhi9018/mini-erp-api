package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultRotationBatch}：暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_rotation_batch", schema = "public", autoResultMap = true)
public class VaultRotationBatch extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 内部轮换任务逻辑 ID
     */
    @TableField("rotation_id")
    private Long rotationId;

    /**
     * 客户端批次 ID，同任务内唯一
     */
    @TableField(value = "batch_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID batchId;

    /**
     * 规范请求 SHA-256 摘要；同批次不同内容必须拒绝
     */
    @TableField("request_digest")
    private byte[] requestDigest;

    /**
     * 本批次条目数量；请求体还需限制为 1 MiB
     */
    @TableField("item_count")
    private Integer itemCount;
}
