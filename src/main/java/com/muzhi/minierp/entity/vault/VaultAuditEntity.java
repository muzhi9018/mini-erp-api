package com.muzhi.minierp.entity.vault;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * <p>
 * {@code VaultAuditEntity}：密码库公共审计字段；操作人由服务端显式设置
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
public abstract class VaultAuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private OffsetDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime gmtModified;

    /**
     * 创建人系统用户 ID，系统任务可为空
     */
    @TableField("create_user")
    private Long createUser;

    /**
     * 最近更新人系统用户 ID，系统任务可为空
     */
    @TableField("update_user")
    private Long updateUser;

    /**
     * 逻辑删除标记
     */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 删除人系统用户 ID，系统任务可为空
     */
    @TableField("delete_user")
    private Long deleteUser;

    /**
     * 逻辑删除时间
     */
    @TableField("gmt_deleted")
    private OffsetDateTime gmtDeleted;
}
