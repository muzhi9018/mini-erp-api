package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import com.muzhi.minierp.vo.vault.VaultRotationStatusVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultRotationExpectedItem}：轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@AutoMapper(target = VaultRotationStatusVO.ExpectedItemVO.class, reverseConvertGenerate = false)
@TableName(value = "vault_rotation_expected_item", schema = "public", autoResultMap = true)
public class VaultRotationExpectedItem extends VaultAuditEntity {

    @Serial
    private static final long serialVersionUID = -183624090733933301L;

    /**
     * 内部轮换任务逻辑 ID
     */
    @TableField("rotation_id")
    private Long rotationId;

    /**
     * 预期目标条目 ID
     */
    @TableField(value = "item_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID itemId;

    /**
     * 轮换源内容版本；备份恢复模式为 NULL
     */
    @TableField("source_content_version")
    private Long sourceContentVersion;

    /**
     * 轮换源行版本；备份恢复模式为 NULL
     */
    @TableField("source_row_version")
    private Long sourceRowVersion;

    /**
     * 完整轮换保持源版本；备份恢复在新上下文中固定从 1 开始
     */
    @TableField("target_content_version")
    @AutoMapping(target = "contentVersion")
    private Long targetContentVersion;
}
