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
 * 完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_rotation")
public class VaultRotation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 公开任务 ID，不代替账户和会话授权
     */
    @TableField("rotation_id")
    private Object rotationId;

    /**
     * 目标密码库
     */
    @TableField("vault_id")
    private Long vaultId;

    /**
     * ROTATION=已有库换密钥，RESTORE=向未初始化或空库恢复备份
     */
    @TableField("mode")
    private String mode;

    /**
     * UPLOADING=可上传验证，其他状态为不可逆终态
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
    @TableField("session_binding_id")
    private Object sessionBindingId;

    /**
     * 登记的预期条目数量；提交还必须检查逐项集合
     */
    @TableField("expected_item_count")
    private Integer expectedItemCount;

    /**
     * 客户端完整下载校验后提交的规范暂存密文清单摘要，不代表服务器验证了明文
     */
    @TableField("verified_manifest_digest")
    private byte[] verifiedManifestDigest;

    /**
     * 仅含提交结果、代次和版本的公开回执，不能含秘密或会话令牌
     */
    @TableField("commit_receipt")
    private Object commitReceipt;

    /**
     * 最长 30 分钟绝对截止时间；二次认证不会延长
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 提交、取消或超时的终态时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

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
