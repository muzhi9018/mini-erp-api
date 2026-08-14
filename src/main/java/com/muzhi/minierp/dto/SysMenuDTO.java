package com.muzhi.minierp.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Mr.Muzhi
 * @since 2023-12-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysMenuDTO implements Serializable {


    @Serial
    private static final long serialVersionUID = -792533572491745101L;

    /**
     * id
     */
    private Long id;

    /**
     * 父级id (顶级菜单为 0)
     */
    private Long parentId;

    /**
     * 层级(记录级联的id层级)
     */
    private String hierarchy;

    /**
     * 所属客户端
     */
    private String clientId;

    /**
     * 1->目录; 2->菜单; 3->按钮; 4->接口
     */
    private Integer type;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 国际化编码
     */
    private String i18nCode;

    /**
     * 路径
     */
    private String path;

    /**
     * 组件
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

    /**
     * 权限码
     */
    private String permissionCode;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    private LocalDateTime gmtModified;

    /**
     * 创建者
     */
    private Long createUser;

    /**
     * 更新者
     */
    private Long updateUser;

    /**
     * 是否删除（false: 否；true: 是）
     */
    private boolean deleted;

}
