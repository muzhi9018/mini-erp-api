package com.muzhi.minierp.client.listener;

import com.alibaba.fastjson2.JSON;
import com.muzhi.minierp.enums.ProgressStatus;
import com.muzhi.minierp.model.Progress;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import lombok.extern.slf4j.Slf4j;
import com.muzhi.minierp.util.Assert;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * {@code BaseProgressListener}: 进度条监听器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2024/2/22 16:34
 */
@Slf4j
public abstract class BaseProgressListener implements ProgressListener {

    /**
     * SseEmitter
     */
    private final SseEmitter sseEmitter;

    /**
     * 进度 id
     */
    private final Long progressId;

    /**
     * title
     */
    private final String title;

    /**
     * 内容长度
     */
    private final long contentLength;

    /**
     * 上次通知的时间
     */
    private long lastSentTime;

    /**
     * 进度条监听器缓存服务
     */
    private volatile IProgressCacheService progressCacheService;


    public BaseProgressListener(Long progressId, String title, Long contentLength) {
        this.sseEmitter = new SseEmitter(-1L);
        this.sseEmitter.onError(throwable -> log.error("sseEmitter on error: {}, cause: {}", throwable.getMessage(), throwable.getCause()));
        this.progressId = progressId;
        this.title = title;
        this.contentLength = contentLength;
    }

    @Override
    public SseEmitter getSseEmitter() {
        return this.sseEmitter;
    }

    @Override
    public void setProgressCacheService(IProgressCacheService progressCacheService) {
        if (this.progressCacheService == null) {
            Assert.isNull(progressCacheService, "设置的进度条缓存服务不能为空");
            this.progressCacheService = progressCacheService;
        }
    }

    protected long getContentLength() {
        return this.contentLength;
    }

    /**
     * 更新进度条
     * @author Mr.Muzhi
     * @since 2024/2/22 17:02
     */
    protected void updateProgress(double percentage, ProgressStatus status) {
        long currentTime = System.currentTimeMillis();
        if (status == ProgressStatus.ACTIVE && (currentTime - this.lastSentTime) < 333L) {
            return;
        }
        this.renewalCache();
        this.lastSentTime = currentTime;
        Progress progress = new Progress();
        progress.setProgressId(this.progressId);
        progress.setTitle(this.title);
        progress.setContentLength(this.contentLength);
        progress.setPercentage(percentage);
        progress.setStatus(status.getStatus());
        try {
            this.sseEmitter.send(JSON.toJSONString(progress));
        } catch (Exception e) {
            log.error("更新进度条时发生异常", e);
        }
    }

    /**
     * 完成
     * @author Mr.Muzhi
     * @since 2024/2/22 16:58
     */
    protected void complete() {
        this.sseEmitter.complete();
    }


    /**
     * 缓存续期
     * @author Mr.Muzhi
     * @since 2024/3/12 11:35
     */
    private void renewalCache() {
        if (this.progressCacheService != null) {
            progressCacheService.getProgressListener(this.progressId);
        }
    }
}
