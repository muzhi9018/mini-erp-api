package com.muzhi.minierp.client;

import com.muzhi.minierp.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * {@code BaseOssClient}: BaseOssClient
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 16:47
 */
@Slf4j
public abstract class BaseOssClient {


    /**
     * URL 转换成字符串
     * @param url url
     * @return 字符串
     */
    protected String urlConvertString(URL url) {
        String urlStr = url.toString();
        boolean existQueryParams = urlStr.contains("?");
        if (existQueryParams) {
            // 使用问号切割
            String[] urlSplit = urlStr.split("\\?");
            StringBuilder urlBuilder = new StringBuilder(URLDecoder.decode(urlSplit[0], StandardCharsets.UTF_8));
            // 拼接后面的参数
            for (int i = 0; i < urlSplit.length; i++) {
                if (i > 0) {
                    urlBuilder.append("?");
                    urlBuilder.append(urlSplit[i]);
                }
            }
            return urlBuilder.toString();
        } else {
            return URLDecoder.decode(urlStr, StandardCharsets.UTF_8);
        }
    }

    /**
     * 文件上传异常处理器
     * @author Mr.Muzhi
     * @since 2026/9/17 17:55
     * @param bucketName Bucket Name
     * @param key OSS Object Key
     * @param e 异常
     */
    protected void uploadObjectExceptionHandler(String bucketName, String key, Exception e) {
        log.error("上传对象时发生异常: bucketName: {}; key: {}", bucketName, key, e);
        throw new BusinessException("system.attachment.upload-failed", "文件上传时发生异常", e);
    }

    /**
     * 通过流上传对象异常处理器
     * @author Mr.Muzhi
     * @since 2026/9/17 17:09
     * @param bucketName Bucket Name
     * @param key OSS Object Key
     * @param e 异常
     */
    protected void uploadStreamExceptionHandler(String bucketName, String key, Exception e) {
        log.error("通过流上传对象时发生异常:; bucketName: {}; key: {}", bucketName, key, e);
        throw new BusinessException("通过流上传对象时发生异常");
    }
}
