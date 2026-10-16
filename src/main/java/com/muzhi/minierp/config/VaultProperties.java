package com.muzhi.minierp.config;

import com.muzhi.minierp.enums.VaultEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.time.Duration;

/**
 * <p>
 * {@code VaultProperties}：密码库受控源与运行参数；默认关闭
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {
    /**
     * 完成迁移并配置域名后才启用
     */
    private boolean enabled;

    /**
     * 唯一允许的 HTTPS 源，不包含路径或末尾斜杠
     */
    private String origin;

    /**
     * WebAuthn RP ID，第一版固定为密码库站点主机名
     */
    private String rpId;

    /**
     * WebAuthn 用户可见站点名称
     */
    private String rpName = "Mini ERP Vault";

    /**
     * 在线逻辑删除密文的物理清理延迟
     */
    private Duration purgeDelay = Duration.ofHours(24);

    /**
     * Redis 通知配置方式，受限 Redis 使用 EXTERNAL，见 {@link VaultEnum.RedisConfiguration}
     */
    private VaultEnum.RedisConfiguration redisConfiguration = VaultEnum.RedisConfiguration.AUTO;

    public void validate() {
        if (!enabled) {
            return;
        }
        URI uri = URI.create(origin == null ? "" : origin);
        boolean valid = "https".equals(uri.getScheme()) && uri.getHost() != null
                && uri.getHost().equals(rpId) && uri.getRawPath().isEmpty()
                && uri.getRawQuery() == null && uri.getRawFragment() == null && uri.getUserInfo() == null;
        if (!valid || rpName == null || rpName.isBlank() || purgeDelay == null || purgeDelay.isNegative() || redisConfiguration == null) {
            throw new IllegalStateException("vault origin/RP ID/configuration is invalid");
        }
    }
}
