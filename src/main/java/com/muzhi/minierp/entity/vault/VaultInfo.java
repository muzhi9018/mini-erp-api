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
 * 个人密码库元数据；一个认证账户仅有一个库
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_info")
public class VaultInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成
     */
    @TableField("id")
    private Long id;

    /**
     * 公开协议库 ID，由客户端生成随机 UUID，以规范小写字符串参与 AAD
     */
    @TableField("vault_id")
    private Object vaultId;

    /**
     * 所属认证账户，所有归属条件从认证会话推导
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * EMPTY=未初始化占位，ACTIVE=可读取活动代，RESTORING=恢复暂存中
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
