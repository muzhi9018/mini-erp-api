package com.muzhi.minierp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;

/**
 * <p>
 * {@code VaultSessionConfig}：仅在密码库路径装配 Redis Session
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 900, redisNamespace = "mini-erp:vault:sessions")
public class VaultSessionConfig {
    @Bean
    public FilterRegistrationBean<com.muzhi.minierp.security.vault.VaultSessionBoundaryFilter> vaultSessionBoundary(
            com.muzhi.minierp.security.vault.VaultErrorWriter errors) {
        var filter = new com.muzhi.minierp.security.vault.VaultSessionBoundaryFilter(errors);
        var registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 1);
        registration.addUrlPatterns("/vault-auth/*", "/api/v1/vault/*");
        return registration;
    }

    @Bean
    public ConfigureRedisAction configureRedisAction(VaultProperties properties) {
        return switch (properties.getRedisConfiguration()) {
            case AUTO -> new ConfigureNotifyKeyspaceEventsAction();
            case EXTERNAL -> ConfigureRedisAction.NO_OP;
        };
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("__Host-vault_session");
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(true);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    public FilterRegistrationBean<SessionRepositoryFilter<?>> vaultSessionFilterRegistration(SessionRepositoryFilter<?> filter) {
        FilterRegistrationBean<SessionRepositoryFilter<?>> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);
        registration.addUrlPatterns("/vault-auth/*", "/api/v1/vault/*");
        return registration;
    }
}
