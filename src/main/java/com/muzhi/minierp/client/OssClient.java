package com.muzhi.minierp.client;

import com.muzhi.minierp.model.ObjectUploaded;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;

/**
 * <p>
 * {@code OssClient}: OSS 客户端
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 16:34
 */
public interface OssClient {

    /**
     * 关闭资源
     * @author Mr.Muzhi
     * @since 2026/9/17 16:39
     */
    void close();

    /**
     * 获取公共下载地址
     * @author Mr.Muzhi
     * @since 2026/9/17 16:39
     * @param bucketName bucket 名称
     * @param key OSS Object Key
     * @return 返回结果
     */
    String getPublicDownloadUrl(String bucketName, String key);

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
     * 上传文件并按调用方提供的进度 ID 缓存传输进度。
     *
     * @param file 文件
     * @param bucketName 存储桶
     * @param key 对象路径
     * @param progressId 进度 ID，允许为空
     * @return 上传完成后的结果
     */
    ObjectUploaded upload(MultipartFile file, String bucketName, String key, Long progressId);

    /**
     * 删除指定对象，用于上传后的失败补偿。
     *
     * @param bucketName 存储桶名称
     * @param key 对象路径
     */
    void deleteObject(String bucketName, String key);

    /**
     * 从输入流上传文件
     * @author Mr.Muzhi
     * @since 2026/9/17 17:04
     * @param inputStream 输入流
     * @param bucketName BucketName
     * @param key key
     * @param progressId 进度 id
     * @return 上传结果
     */
    ObjectUploaded uploadStream(InputStream inputStream, String bucketName, String key, Long progressId);
}
