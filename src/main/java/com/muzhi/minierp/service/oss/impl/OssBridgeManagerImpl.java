package com.muzhi.minierp.service.oss.impl;

import com.muzhi.minierp.client.OssClient;
import com.muzhi.minierp.enums.RedisKey;
import com.muzhi.minierp.model.ObjectUploaded;
import com.muzhi.minierp.service.oss.IOssBridgeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * <p>
 * {@code OssBridgeManagerImpl}: 对象存储 Bridge 管理器实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 14:58
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssBridgeManagerImpl implements IOssBridgeManager {


    private final OssClient ossClient;

    private final RedisTemplate<String, String> redisTemplate;


    @Override
    public String getAuthorizedDownloadUrl(String bucketName, String key, Duration authorizationDuration) {
        return ossClient.getAuthorizedDownloadUrl(bucketName, key, authorizationDuration);
    }

    @Override
    public String getAuthorizedDownloadUrl(String bucketName, String key) {
        RedisKey.Oss redisObjCacheKey = RedisKey.Oss.OBJECT_URL_CACHE;
        String redisKey = redisObjCacheKey.getKey(bucketName, key);
        String url = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotEmpty(url)) {
            return url;
        }
        Duration timeout = redisObjCacheKey.getTimeout();
        String downloadUrl = this.getAuthorizedDownloadUrl(bucketName, key, timeout.plusMinutes(5));
        redisTemplate.opsForValue().set(redisKey, downloadUrl, timeout);
        return downloadUrl;
    }

    @Override
    public void deleteObject(String bucketName, String key) {
        ossClient.deleteObject(bucketName, key);
        RedisKey.Oss redisObjCacheKey = RedisKey.Oss.OBJECT_URL_CACHE;
        String redisKey = redisObjCacheKey.getKey(bucketName, key);
        redisTemplate.delete(redisKey);
    }

    @Override
    public ObjectUploaded upload(MultipartFile file, String bucketName, String key, Long fileId) {
        return ossClient.upload(file, bucketName, key, fileId);
    }
}
