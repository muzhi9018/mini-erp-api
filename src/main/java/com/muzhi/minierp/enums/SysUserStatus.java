package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * 系统用户状态。
 */
@Getter
public enum SysUserStatus {

    ENABLED(1, "正常"),

    DISABLED(0, "禁用");

    private final int value;

    private final String description;

    SysUserStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int value() {
        return this.value;
    }

    public boolean eq(Integer value) {
        return value != null && this.value == value;
    }

    public static boolean isEnabled(Integer value) {
        return ENABLED.eq(value);
    }
}
