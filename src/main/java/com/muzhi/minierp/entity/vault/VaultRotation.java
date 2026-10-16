package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import com.muzhi.minierp.vo.vault.VaultRotationVO;
import com.muzhi.minierp.vo.vault.VaultRotationStatusVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultRotation}：完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@AutoMappers({
        @AutoMapper(target = VaultRotationVO.class, reverseConvertGenerate = false),
        @AutoMapper(target = VaultRotationStatusVO.class, reverseConvertGenerate = false)
})
@TableName(value = "vault_rotation", schema = "public", autoResultMap = true)
public class VaultRotation extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 公开任务 ID，不代替账户和会话授权
     */
    @TableField(value = "rotation_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID rotationId;

    /**
     * 目标密码库
     */
    @TableField("vault_id")
    private Long vaultId;

    /**
     * ROTATION=已有库换密钥，RESTORE=向未初始化或空库恢复备份
     * {@link com.muzhi.minierp.enums.VaultEnum.RotationMode}
     */
    @TableField("mode")
    private String mode;

    /**
     * UPLOADING=可上传验证，其他状态为不可逆终态
     * {@link com.muzhi.minierp.enums.VaultEnum.RotationStatus}
     */
    @TableField("status")
    private String status;

    /**
     * 开始时的活动代；目标未初始化时为 NULL
     */
    @TableField("source_generation")
    private Long sourceGeneration;

    /**
     * 全新暂存代，取消后不复用
     */
    @TableField("target_generation")
    @AutoMapping(target = "generation")
    private Long targetGeneration;

    /**
     * 开始时锁定的元数据版本
     */
    @TableField("expected_meta_revision")
    private Long expectedMetaRevision;

    /**
     * 开始时锁定的集合版本
     */
    @TableField("expected_collection_revision")
    private Long expectedCollectionRevision;

    /**
     * 开始时的认证代次；外部敏感变更后禁止旧任务继续提交
     */
    @TableField("started_auth_epoch")
    private Long startedAuthEpoch;

    /**
     * 服务端生成的逻辑会话标识；会话 ID 更新时保持，不是 Cookie 或 Session ID
     */
    @TableField(value = "session_binding_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID sessionBindingId;

    /**
     * 登记的预期条目数量；提交还必须检查逐项集合
     */
    @TableField("expected_item_count")
    @AutoMapping(targetClass = VaultRotationStatusVO.class, target = "expectedItemCount", ignore = true)
    private Integer expectedItemCount;

    /**
     * 客户端完整下载校验后提交的规范暂存密文清单摘要，不代表服务器验证了明文
     */
    @TableField("verified_manifest_digest")
    private byte[] verifiedManifestDigest;

    /**
     * 仅含提交结果、代次和版本的公开回执，不能含秘密或会话令牌
     */
    @TableField(value = "commit_receipt", typeHandler = VaultJsonTypeHandler.class)
    @AutoMapping(targetClass = VaultRotationStatusVO.class, target = "commitReceipt", ignore = true)
    private String commitReceipt;

    /**
     * 最长 30 分钟绝对截止时间；二次认证不会延长
     */
    @TableField("expires_at")
    @AutoMapping(target = "expiresAt", ignore = true)
    private OffsetDateTime expiresAt;

    /**
     * 提交、取消或超时的终态时间
     */
    @TableField("finished_at")
    private OffsetDateTime finishedAt;
}
