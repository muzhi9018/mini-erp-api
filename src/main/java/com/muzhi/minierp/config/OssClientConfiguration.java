package com.muzhi.minierp.config;

import com.muzhi.minierp.client.OssClient;
import com.muzhi.minierp.client.S3OssClient;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

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

    @Bean
    public OssClient ossClient(OssConfigProperties properties, IProgressCacheService progressCacheService) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolFactory.getOssThreadPoolExecutor();
        long minimumPartSizeInBytes = 8L * 1024 * 1024;
        int uploadRetryCount = 3;
        long minimumThroughputInBps = 1L;
        Duration minimumThroughputTimeout = Duration.ofSeconds(30);

        S3AsyncClient s3AsyncClient = S3AsyncClient.crtBuilder()
                .endpointOverride(URI.create(properties.getInternalEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(credentialsProvider)
                .forcePathStyle(true)
                .minimumPartSizeInBytes(minimumPartSizeInBytes)
                .futureCompletionExecutor(threadPoolExecutor)
                .retryConfiguration(builder -> builder.numRetries(uploadRetryCount))
                .httpConfiguration(builder -> builder.connectionHealthConfiguration(healthBuilder -> {
                    healthBuilder.minimumThroughputInBps(minimumThroughputInBps);
                    healthBuilder.minimumThroughputTimeout(minimumThroughputTimeout);
                }))
                .build();

        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();

        S3Utilities s3Utilities = S3Utilities.builder()
                .endpoint(URI.create(properties.getExternalEndpoint()))
                .region(Region.AP_EAST_1)
                .build();

        S3Presigner s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(properties.getExternalEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();

        return new S3OssClient(s3AsyncClient, s3Utilities, s3Presigner, transferManager, progressCacheService);
    }

    @Override
    public void destroy() {
        ThreadPoolFactory.shutdownOssThreadPoolExecutor();
    }
}
