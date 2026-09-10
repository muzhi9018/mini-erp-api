package com.muzhi.minierp.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * {@code SysLocale}: 系统区域枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/10 15:20
 */
@Getter
public enum SysLocale {

    ZH_CN(1, "zh-CN", "Simplified Chinese", "简体中文", true,  Locale.SIMPLIFIED_CHINESE),
    ZH_TW(2, "zh-TW", "Traditional Chinese", "繁體中文", false, Locale.TRADITIONAL_CHINESE),
    EN_US(3, "en-US", "English","English", false, Locale.US),
    ;

    private final int id;


    private final String code;

    private final String name;

    private final String nativeName;

    private final boolean defaultLocal;

    private final Locale locale;

    SysLocale(int id, String code, String name, String nativeName, boolean defaultLocal, Locale locale) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.nativeName = nativeName;
        this.defaultLocal = defaultLocal;
        this.locale = locale;
    }

    /**
     * 获取所有区域
     * @author Mr.Muzhi
     * @since 2026/9/10 15:28
     * @return 区域列表
     */
    public static List<Locale> locales() {
        SysLocale[] values = SysLocale.values();
        List<Locale> locales = new ArrayList<>();
        for (SysLocale value : values) {
            locales.add(value.getLocale());
        }
        return locales;
    }

    /**
     * 获得默认系统区域
     * @author Mr.Muzhi
     * @since 2026/9/10 15:29
     * @return 默认的系统区域
     */
    public static Locale defaultLocale() {
        return ZH_CN.getLocale();
    }

    public static SysLocale ofCode(String code) {
        for (SysLocale value : SysLocale.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
