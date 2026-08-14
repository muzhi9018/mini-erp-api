package com.muzhi.minierp.enums;

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
    enum User implements RedisKey {

        USER_AUTHORITIES("USER_AUTHORITIES",  Duration.ofHours(1));

        private final String key;

        private final Duration timeout;


        User(String key, Duration timeout) {
            this.key = key;
            this.timeout = timeout;
        }

        @Override
        public String getKey(String keyId) {
            return this.key + ":" + keyId;
        }

        @Override
        public Duration getTimeout() {
            return this.timeout;
        }
    }

    /**
     * 获取 Redis key
     * @author Mr.Muzhi
     * @since 2023/12/9 09:18
     * @param keyId key id 唯一标识
     * @return 返回结果
     */
    String getKey(String keyId);

    /**
     * 获取过期时间
     * @author Mr.Muzhi
     * @since 2023/12/9 09:18
     * @return 返回过期时间
     */
    Duration getTimeout();

}
