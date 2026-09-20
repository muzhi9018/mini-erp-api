package com.muzhi.minierp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * {@code OssConfigProperties}: OSS 配置
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 16:23
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfigProperties {

    /**
     * AccessKey
     */
    private String accessKey;

    /**
     * SecretKey
     */
    private String secretKey;

    /**
     * BucketName
     */
    private String bucketName;

    /**
     * 內部端点
     */
    private String internalEndpoint;

    /**
     * 外部端点
     */
    private String externalEndpoint;

}
