package com.muzhi.minierp.config;

import com.muzhi.minierp.client.OssClient;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import com.muzhi.minierp.client.S3OssClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * {@code OssClientConfiguration}: OSS 客户端配置
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 17:59
 */
@Configuration
public class OssClientConfiguration implements DisposableBean {

    private volatile SdkAsyncHttpClient httpClient = null;

    @Bean
    public OssClient ossClient(OssConfigProperties properties, IProgressCacheService progressCacheService) {
        this.initHttpClient();
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolFactory.getOssThreadPoolExecutor();

        S3AsyncClient internalS3AsyncClient = S3AsyncClient.builder()
                .httpClient(httpClient)
                .asyncConfiguration(builder -> builder.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, threadPoolExecutor))
                .endpointOverride(URI.create(properties.getInternalEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(() -> awsBasicCredentials)
                .forcePathStyle(true)
                .build();

        S3AsyncClient externalS3AsyncClient = S3AsyncClient.builder()
                .httpClient(httpClient)
                .asyncConfiguration(builder -> builder.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, threadPoolExecutor))
                .endpointOverride(URI.create(properties.getExternalEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(() -> awsBasicCredentials)
                .build();

        S3Presigner s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(properties.getExternalEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();

        return new S3OssClient(internalS3AsyncClient, externalS3AsyncClient, s3Presigner, progressCacheService);

    }

    private void initHttpClient() {
        if (this.httpClient == null) {
            synchronized (OssClientConfiguration.class) {
                if (this.httpClient == null) {
                    this.httpClient = AwsCrtAsyncHttpClient.builder()
                            .connectionHealthConfiguration(builder -> {
                                builder.minimumThroughputInBps(32000L);
                                builder.minimumThroughputTimeout(Duration.ofSeconds(3));
                            })
                            .connectionMaxIdleTime(Duration.ofSeconds(5))
                            .build();
                }
            }
        }
    }

    @Override
    public void destroy() {
        try {
            if (this.httpClient != null) {
                this.httpClient.close();
            }
        } finally {
            ThreadPoolFactory.shutdownOssThreadPoolExecutor();
        }
    }
}
