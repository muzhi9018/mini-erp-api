package com.muzhi.minierp.client.listener;

import com.muzhi.minierp.model.Progress;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * {@code ProgressListener}: 进度条监听器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2024/2/22 16:33
 */
public interface ProgressListener {

    /**
     * 获取 SseEmitter
     * @author Mr.Muzhi
     * @since 2024/2/22 17:21
     * @return 进度监听器
     */
    SseEmitter getSseEmitter();

    /**
     * 设置进度条缓存服务
     * @author Mr.Muzhi
     * @since 2024/3/12 11:37
     * @param progressCacheService progressCacheService
     */
    void setProgressCacheService(IProgressCacheService progressCacheService);
}
