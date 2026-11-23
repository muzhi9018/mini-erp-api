package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * 报价和模板业务枚举。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public interface QuotationEnum {

    @Getter
    enum Scope {
        PERSONAL("PERSONAL", "个人"),
        PUBLIC("PUBLIC", "公共");

        private final String code;

        private final String name;

        Scope(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
