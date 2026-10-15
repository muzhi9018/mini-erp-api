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
 * 暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_rotation_batch")
public class VaultRotationBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 内部轮换任务逻辑 ID
     */
    @TableField("rotation_id")
    private Long rotationId;

    /**
     * 客户端批次 ID，同任务内唯一
     */
    @TableField("batch_id")
    private Object batchId;

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
