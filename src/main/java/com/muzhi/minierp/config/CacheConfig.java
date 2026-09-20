package com.muzhi.minierp.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>
 * {@code CacheConfig}: 缓存配置
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 17:15
 */
@Configuration
public class CacheConfig {

    /**
     * SSE 心跳线程随应用关闭，取消的任务及时从队列移除。
     */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ossProgressHeartbeatExecutor() {
        var executor = new java.util.concurrent.ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "minierp-oss-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Cache<Object, Object> progressListenerCache = this.builderProgressListenerCache();
        cacheManager.registerCustomCache("progressListenerCache", progressListenerCache);
        return cacheManager;
    }

    private Cache<Object, Object> builderProgressListenerCache() {
        Caffeine<Object, Object> caffeine = Caffeine
                .newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterAccess(30, TimeUnit.MINUTES);
        return caffeine.build();
    }
}
