package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code AttachmentEnum}: 附件存储与访问类型
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-18
 */
public interface AttachmentEnum {

    @Getter
    enum StorageType {

        RUSTFS("RUSTFS", "RustFS 对象存储");

        private final String code;

        private final String name;

        StorageType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum AccessType {

        PUBLIC("public", "公开访问"),
        PRIVATE("private", "私有访问");

        private final String code;

        private final String name;

        AccessType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
