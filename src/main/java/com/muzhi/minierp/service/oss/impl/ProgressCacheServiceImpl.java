package com.muzhi.minierp.service.oss.impl;

import com.muzhi.minierp.client.listener.ProgressListener;
import com.muzhi.minierp.client.listener.S3OssClientProgressListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * <p>
 * {@code ProgressCacheServiceImpl}: 按当前用户隔离的上传进度缓存
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2024/3/12 11:22
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "progressListenerCache")
public class ProgressCacheServiceImpl implements IProgressCacheService {


    @Override
    @CachePut(key = "#progressId")
    public ProgressListener saveProgressListener(Long progressId, ProgressListener progressListener) {
        return progressListener;
    }


    @Override
    @Cacheable(key = "#progressId", unless = "#result == null")
    public ProgressListener getProgressListener(Long progressId) {
        return null;
    }

    @Override
    @CacheEvict(key = "#progressId")
    public void removeProgressListener(Long progressId) {
        log.info("ProgressListener 执行移除缓存操作 progressId: {}", progressId);
    }
}
