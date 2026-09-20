package com.muzhi.minierp.client;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.muzhi.minierp.client.listener.S3OssClientProgressListener;
import com.muzhi.minierp.config.ThreadPoolFactory;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import com.muzhi.minierp.model.ObjectUploaded;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

/**
 * <p>
 * {@code S3OssClient}:  Oss S3 客户端
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 16:42
 */
@Slf4j
public final class S3OssClient extends BaseOssClient implements OssClient {

    private final S3AsyncClient internalS3AsyncClient;

    private final S3AsyncClient externalS3AsyncClient;

    private final S3Presigner s3Presigner;

    private final IProgressCacheService progressCacheService;


    public S3OssClient(S3AsyncClient internalS3AsyncClient, S3AsyncClient externalS3AsyncClient, S3Presigner s3Presigner, IProgressCacheService progressCacheService) {
        this.internalS3AsyncClient = internalS3AsyncClient;
        this.externalS3AsyncClient = externalS3AsyncClient;
        this.s3Presigner = s3Presigner;
        this.progressCacheService = progressCacheService;

    }

    @Override
    public void close() {
        try {
            externalS3AsyncClient.close();
        } finally {
            try {
                internalS3AsyncClient.close();
            } finally {
                s3Presigner.close();
            }
        }
    }

    @Override
    public String getPublicDownloadUrl(String bucketName, String key) {
        GetUrlRequest request = GetUrlRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build();
        URL url = externalS3AsyncClient.utilities().getUrl(request);
        return super.urlConvertString(url);
    }

    @Override
    public String getAuthorizedDownloadUrl(String bucketName, String key, Duration authorizationDuration) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(authorizationDuration)
                .getObjectRequest(objectRequest)
                .build();
        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(objectPresignRequest);
        URL url = presignedGetObjectRequest.url();
        return super.urlConvertString(url);
    }

    @Override
    public void deleteObject(String bucketName, String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        internalS3AsyncClient.deleteObject(request).join();
    }

    @Override
    public ObjectUploaded upload(MultipartFile file, String bucketName, String key, Long fileId) {
        ObjectUploaded uploaded = new ObjectUploaded();
        uploaded.setProgressId(fileId);
        uploaded.setBucketName(bucketName);
        uploaded.setKey(key);
        S3OssClientProgressListener progressListener = new S3OssClientProgressListener(uploaded.getProgressId(), file.getOriginalFilename(), file.getSize());
        progressCacheService.saveProgressListener(uploaded.getProgressId(), progressListener);
        uploaded.setProgressListener(progressListener);
        try (InputStream inputStream = file.getInputStream();
             S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(internalS3AsyncClient)
                .build()) {
            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            AsyncRequestBody requestBody = AsyncRequestBody
                    .fromInputStream(inputStream, file.getSize(), ThreadPoolFactory.getOssThreadPoolExecutor());
            UploadRequest uploadRequest = UploadRequest.builder()
                    .requestBody(requestBody)
                    .putObjectRequest(request)
                    .addTransferListener(progressListener)
                    .build();
            Upload upload = transferManager.upload(uploadRequest);
            CompletedUpload completedUpload = upload.completionFuture().join();
            PutObjectResponse response = completedUpload.response();
            uploaded.setEtag(response.eTag());
        } catch (Exception e) {
            super.uploadObjectExceptionHandler(bucketName, key, e);
        }
        return uploaded;
    }

    @Override
    public ObjectUploaded uploadStream(InputStream inputStream, String bucketName, String key, Long progressId) {
        ObjectUploaded uploaded = new ObjectUploaded();
        uploaded.setProgressId(progressId);
        uploaded.setBucketName(bucketName);
        uploaded.setKey(key);

        try (S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(internalS3AsyncClient)
                .build()) {
            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // 使用AsyncRequestBody从输入流创建请求体
            // 注意：由于无法预先知道流的大小，设置为null表示未知大小
            AsyncRequestBody requestBody = AsyncRequestBody
                    .fromInputStream(inputStream, null, ThreadPoolFactory.getOssThreadPoolExecutor());

            // 创建进度监听器，设置大小为null表示未知大小
            S3OssClientProgressListener progressListener = new S3OssClientProgressListener(uploaded.getProgressId(), key, 0L);
            uploaded.setProgressListener(progressListener);

            // 创建并执行上传请求
            UploadRequest uploadRequest = UploadRequest.builder()
                    .requestBody(requestBody)
                    .putObjectRequest(request)
                    .addTransferListener(progressListener)
                    .build();

            transferManager.upload(uploadRequest);
            log.info("开始通过流上传文件至OSS，bucket: {}, key: {}, progressId: {}", bucketName, key, progressId);
        } catch (Exception e) {
            super.uploadStreamExceptionHandler(bucketName, key, e);
        }

        return uploaded;
    }
}
