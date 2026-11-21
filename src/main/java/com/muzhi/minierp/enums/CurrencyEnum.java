package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code CurrencyEnum}: 币种
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/11/20 16:13
 */
@Getter
public enum CurrencyEnum {

    /**
     * 人民币
     */
    CNY("人民币", "CNY", "¥"),

    /**
     * 新台币
     */
    TWD("新台币", "TWD", "NT$"),
    ;

    private final String name;

    private final String code;

    private final String emoji;


    CurrencyEnum(String name, String code, String emoji) {
        this.name = name;
        this.code = code;
        this.emoji = emoji;
    }

    /**
     * 查询合法币种编码，未知编码返回 null。
     */
    public static CurrencyEnum ofCode(String code) {
        for (CurrencyEnum value : CurrencyEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
