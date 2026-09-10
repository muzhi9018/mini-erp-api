package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code WebsiteProductEnum}: 官网商品枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/10 16:53
 */
public interface WebsiteProductEnum {


    @Getter
    enum DetailItemType {

        FEATURE("FEATURE", "特点"),


        SPECIFICATION("SPECIFICATION", "技术参数"),

        ;

        private final String code;

        private final String name;

        DetailItemType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum MediaItemType {


        APPLICATION("APPLICATION", "应用场景"),


        CASE("CASE", "案例展示"),
        ;

        private final String code;

        private final String name;

        MediaItemType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
