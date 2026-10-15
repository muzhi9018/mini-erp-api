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
 * 轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ToString
@TableName("vault_rotation_expected_item")
public class VaultRotationExpectedItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部轮换任务逻辑 ID
     */
    @TableField("rotation_id")
    private Long rotationId;

    /**
     * 预期目标条目 ID
     */
    @TableField("item_id")
    private Object itemId;

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
    private Long targetContentVersion;

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
