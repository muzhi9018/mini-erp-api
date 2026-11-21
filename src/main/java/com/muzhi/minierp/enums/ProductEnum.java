package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * 内部商品业务枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductEnum {

    @Getter
    enum Status {
        ENABLED("ENABLED", "启用"),
        DISABLED("DISABLED", "停用");

        /**
         * 持久化及接口编码
         */
        private final String code;

        /**
         * 中文名称
         */
        private final String name;

        Status(String code, String name) {
            this.code = code;
            this.name = name;
        }

        /**
         * 查询合法状态编码，未知编码返回 null。
         */
        public static Status ofCode(String code) {
            for (Status status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }

    @Getter
    enum SpecValueType {
        TEXT("TEXT", "文本"),
        NUMBER("NUMBER", "数值");

        /**
         * 持久化及接口编码
         */
        private final String code;

        /**
         * 中文名称
         */
        private final String name;

        SpecValueType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        /**
         * 查询合法规格类型，未知编码返回 null。
         */
        public static SpecValueType ofCode(String code) {
            for (SpecValueType type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 商品图片归属及快照图片来源
     */
    @Getter
    enum ImageType {
        PRODUCT("PRODUCT", "商品图片"),
        SKU("SKU", "SKU 图片");

        /**
         * 持久化及接口编码
         */
        private final String code;

        /**
         * 中文名称
         */
        private final String name;

        ImageType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
