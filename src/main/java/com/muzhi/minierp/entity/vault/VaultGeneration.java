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
 * {@code VaultGeneration}：密码库代次控制记录；清理密文后保留最小记录供轮换回执引用
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_generation", schema = "public", autoResultMap = true)
public class VaultGeneration extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = -5097021877972355785L;

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
     * 库内单调递增代次，取消后不重复使用
     */
    @TableField("generation")
    private Long generation;

    /**
     * 客户端密钥代次；普通修改主密码保持不变
     */
    @TableField("key_epoch")
    private Long keyEpoch;

    /**
     * STAGING=待提交，ACTIVE=活动，RETIRED=旧代，ABORTED=取消或超时
     * {@link com.muzhi.minierp.enums.VaultEnum.GenerationStatus}
     */
    @TableField("status")
    private String status;

    /**
     * 退出活动或暂存状态的时间，供密文清理任务使用
     */
    @TableField("retired_at")
    private OffsetDateTime retiredAt;
}
