package com.muzhi.minierp.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.muzhi.minierp.vo.SysMenuVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统菜单
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Getter
@Setter
@ToString
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 3817360516910674383L;

    /**
     * id
     */
    @TableId("id")
    private Long id;

    /**
     * 父级id（顶级菜单为 0）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 层级（记录级联的id层级）
     */
    @TableField("hierarchy")
    private String hierarchy;

    /**
     * 所属客户端
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 1->目录; 2->菜单; 3->按钮; 4->接口
     */
    @TableField("type")
    private Short type;

    /**
     * 菜单名称
     */
    @TableField("name")
    private String name;

    /**
     * 国际化编码
     */
    @TableField("i18n_code")
    private String i18nCode;

    /**
     * 路径
     */
    @TableField("path")
    private String path;

    /**
     * 组件
     */
    @TableField("component")
    private String component;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 权限码
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 创建者
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 更新者
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 是否删除（0:否; 1:是）
     */
    @TableLogic
    @TableField("is_deleted")
    private boolean deleted;
}
