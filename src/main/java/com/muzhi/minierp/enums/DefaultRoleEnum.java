package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code DefaultRoleEnum}: 默认角色枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/12/19 11:12
 */
@Getter
public enum DefaultRoleEnum {

    /**
     * 超级管理员
     */
    SUPER_ADMIN(1679771069714911233L, "超级管理员", "SUPER_ADMIN", "牧之云系统超级管理员"),

    /**
     * 系统监控管理员
     */
    SYSTEM_MONITOR_ADMIN(1694996817341935617L, "系统监控管理员", "SYSTEM_MONITOR_ADMIN", "系统监控管理员");

    private final long id;

    private final String roleName;

    private final String roleCode;

    private final String remark;

    DefaultRoleEnum(long id, String roleName, String roleCode, String remark) {
        this.id = id;
        this.roleName = roleName;
        this.roleCode = roleCode;
        this.remark = remark;
    }

    public static DefaultRoleEnum ofCode(String roleCode) {
        for (DefaultRoleEnum value : DefaultRoleEnum.values()) {
            if (value.roleCode.equalsIgnoreCase(roleCode)) {
                return value;
            }
        }
        return null;
    }
}
