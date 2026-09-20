package com.muzhi.minierp.service.oss;

import com.muzhi.minierp.client.listener.ProgressListener;
import com.muzhi.minierp.client.listener.S3OssClientProgressListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * {@code IProgressCacheService}: 进度条缓存服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2024/3/12 11:19
 */
public interface IProgressCacheService {

    /**
     * 缓存进度条
     * @author Mr.Muzhi
     * @since 2024/3/12 11:10
     * @param progressId 进度条 id
     * @param progressListener 进度条监听器
     */
    ProgressListener saveProgressListener(Long progressId, ProgressListener progressListener);

    /**
     * 获取上传进度
     * @author Mr.Muzhi
     * @since 2024/2/22 17:59
     * @param id id
     * @return 返回结果
     */
    ProgressListener getProgressListener(Long id);

    /**
     * 移除缓存
     * @author Mr.Muzhi
     * @since 2024/3/12 11:41
     * @param progressId 缓存 id
     */
    void  removeProgressListener(Long progressId);

}
