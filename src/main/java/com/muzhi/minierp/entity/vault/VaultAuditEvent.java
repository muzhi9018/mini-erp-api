package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultAuditEvent}：认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_audit_event", schema = "public", autoResultMap = true)
public class VaultAuditEvent extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 可识别时的认证账户；未知账户登录失败可以为空
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 所属密码库；初始化前的认证事件可以为空
     */
    @TableField("vault_id")
    private Long vaultId;

    /**
     * 固定审计动作；不允许客户端指定任意动作字符串
     * {@link com.muzhi.minierp.enums.VaultEnum.AuditAction}
     */
    @TableField("action")
    private String action;

    /**
     * 审计结果
     * {@link com.muzhi.minierp.enums.VaultEnum.AuditResult}
     */
    @TableField("result")
    private String result;

    /**
     * 服务端固定机器错误码；不拼接用户输入
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 服务端生成的请求追踪 ID
     */
    @TableField(value = "request_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID requestId;
}
