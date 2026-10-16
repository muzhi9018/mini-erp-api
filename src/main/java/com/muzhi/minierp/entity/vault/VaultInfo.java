package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import com.muzhi.minierp.mapper.vault.type.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * <p>
 * {@code VaultInfo}：个人密码库元数据；一个认证账户仅有一个库
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@TableName(value = "vault_info", schema = "public", autoResultMap = true)
public class VaultInfo extends VaultAuditEntity {

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 公开协议库 ID，由客户端生成随机 UUID，以规范小写字符串参与 AAD
     */
    @TableField(value = "vault_id", typeHandler = VaultUuidTypeHandler.class)
    private UUID vaultId;

    /**
     * 所属认证账户，所有归属条件从认证会话推导
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * EMPTY=未初始化占位，ACTIVE=可读取活动代，RESTORING=恢复暂存中
     * {@link com.muzhi.minierp.enums.VaultEnum.VaultStatus}
     */
    @TableField("status")
    private String status;

    /**
     * 当前活动代；空库占位为 NULL，逻辑关联 vault_generation，由 Service 事务校验
     */
    @TableField("active_generation")
    private Long activeGeneration;

    /**
     * 元数据与活动密钥槽版本
     */
    @TableField("meta_revision")
    private Long metaRevision;

    /**
     * 当前代条目或密钥槽集合版本
     */
    @TableField("collection_revision")
    private Long collectionRevision;
}
