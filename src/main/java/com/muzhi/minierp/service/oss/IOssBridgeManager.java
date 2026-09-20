package com.muzhi.minierp.service.oss;

import com.muzhi.minierp.model.ObjectUploaded;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * <p>
 * {@code IOssBridgeManager}: OSS 对象存储 Bridge 管理器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 14:56
 */
public interface IOssBridgeManager {

    /**
     * 获取授权下载链接
     * @author Mr.Muzhi
     * @since 2026/9/17 14:57
     * @param bucketName bucket 名称
     * @param key OSS Object Key
     * @param authorizationDuration 授权时长
     * @return 返回结果
     */
    String getAuthorizedDownloadUrl(String bucketName, String key, Duration authorizationDuration);

    /**
     * 获取授权下载链接
     * @author Mr.Muzhi
     * @since 2026/9/17 15:00
     * @param bucketName bucket 名称
     * @param key OSS Object Key
     * @return 返回结果
     */
    String getAuthorizedDownloadUrl(String bucketName, String key);

    /**
     * 上传文件并按调用方提供的进度 ID 缓存传输进度。
     * @param file 文件
     * @param bucketName 存储桶
     * @param key 对象路径
     * @param fileId 文件 id
     * @return 上传完成后的结果
     */
    ObjectUploaded upload(MultipartFile file, String bucketName, String key, Long fileId);

    /**
     * 删除指定对象，用于上传后的失败补偿。
     *
     * @param bucketName 存储桶名称
     * @param key 对象路径
     */
    void deleteObject(String bucketName, String key);
}
