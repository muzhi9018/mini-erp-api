package com.muzhi.minierp.enums;

import lombok.Getter;

import java.time.Duration;

/**
 * <p>
 * {@code RedisKey}: Redis Key
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/12/9 08:52
 */
public interface RedisKey {

    /**
     * 用户模块
     * @author Mr.Muzhi
     * @since 2023/12/9 09:16
     */
    @Getter
    enum User implements RedisKey {

        USER_AUTHORITIES("USER_AUTHORITIES",  Duration.ofHours(24));

        private final String baseKey;

        private final Duration timeout;

        User(String baseKey, Duration timeout) {
            this.baseKey = baseKey;
            this.timeout = timeout;
        }
    }

    /**
     * OSS 模块
     */
    @Getter
    enum Oss implements RedisKey {

        OBJECT_URL_CACHE("OBJECT_URL_CACHE",  Duration.ofHours(1));

        private final String baseKey;

        private final Duration timeout;

        Oss(String baseKey, Duration timeout) {
            this.baseKey = baseKey;
            this.timeout = timeout;
        }
    }

    /**
     * 获取BaseKey
     * @author Mr.Muzhi
     * @since 2026/9/23 18:08
     * @return BaseKey
     */
    String getBaseKey();

    /**
     * 获取过期时间
     * @author Mr.Muzhi
     * @since 2026/9/23 18:08
     * @return 返回过期时间
     */
    Duration getTimeout();

    /**
     * 获取 Redis key
     * @author Mr.Muzhi
     * @since 2023/12/9 09:18
     * @param keyId key id 唯一标识
     * @return 返回结果
     */
    default String getKey(String keyId) {
        String baseKey = this.getBaseKey();
        return baseKey + ":"  + keyId;
    }


    /**
     * 获取 Redis key
     * @author Mr.Muzhi
     * @since 2026/9/23 18:00
     * @param args args
     * @return 返回结果
     */
    default String getKey(String ... args) {
        String baseKey = this.getBaseKey();
        StringBuilder sb = new StringBuilder(baseKey);
        for (String arg : args) {
            sb.append(":").append(arg);
        }
        return sb.toString();
    }


}
