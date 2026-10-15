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
 * 业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_idempotency_record")
public class VaultIdempotencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 所属认证账户
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 业务操作类型；批次幂等使用 rotation_batch
     */
    @TableField("operation")
    private String operation;

    /**
     * 客户端随机幂等键
     */
    @TableField("idempotency_key")
    private Object idempotencyKey;

    /**
     * 绑定账户、操作及规范业务参数的 SHA-256 摘要
     */
    @TableField("request_digest")
    private byte[] requestDigest;

    /**
     * 已成功提交结果的真实 HTTP 状态
     */
    @TableField("http_status")
    private Short httpStatus;

    /**
     * 仅保存最小公开 JSON 回执；禁止保存任何解密秘密、MFA 恢复码或会话令牌
     */
    @TableField("response_body")
    private Object responseBody;

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
