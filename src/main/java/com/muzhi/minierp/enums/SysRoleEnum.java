package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code SysRoleEnum}: 系统角色枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/29
 */
public interface SysRoleEnum {

    /**
     * 角色状态。
     */
    @Getter
    enum Status {

        ENABLED((short) 1, "正常"),

        DISABLED((short) 0, "禁用");

        private final short value;

        private final String description;

        Status(short value, String description) {
            this.value = value;
            this.description = description;
        }
    }
}
