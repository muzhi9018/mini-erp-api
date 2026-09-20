package com.muzhi.minierp.client.listener;

import com.muzhi.minierp.enums.ProgressStatus;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalDouble;

/**
 * <p>
 * {@code S3OssClientProgressListener}: S3 OSS 客户端传输进度监听器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 17:07
 */
public class S3OssClientProgressListener extends BaseProgressListener implements TransferListener {


    public S3OssClientProgressListener(Long progressId, String title, Long contentLength) {
        super(progressId, title, contentLength);
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        OptionalDouble ratioTransferred = context.progressSnapshot().ratioTransferred();
        double ratio = ratioTransferred.orElse(0.0);
        this.updateProgressHandler(ratio, ProgressStatus.INITIATED);
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        OptionalDouble ratioTransferred = context.progressSnapshot().ratioTransferred();
        ratioTransferred.ifPresent(ratio -> this.updateProgressHandler(ratio, ProgressStatus.ACTIVE));
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        OptionalDouble ratioTransferred = context.progressSnapshot().ratioTransferred();
        double ratio = ratioTransferred.orElse(1.0);
        this.updateProgressHandler(ratio, ProgressStatus.COMPLETE);
        super.complete();
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        OptionalDouble ratioTransferred = context.progressSnapshot().ratioTransferred();
        double ratio = ratioTransferred.orElse(0.0);
        this.updateProgressHandler(ratio, ProgressStatus.FAILED);
        super.complete();
    }

    /**
     * 更新进度处理器
     * @author Mr.Muzhi
     * @since 2024/3/12 14:44
     * @param ratio 计算传输进度百分比
     * @param progressStatus 传输状态
     */
    private void updateProgressHandler(double ratio, ProgressStatus progressStatus) {
        double percentage = this.calculatePercentage(ratio);
        super.updateProgress(percentage, progressStatus);
    }

    /**
     * 计算传输进度百分比
     * @author Mr.Muzhi
     * @since 2024/3/12 14:44
     * @param ratio 传输比例
     * @return 返回结果
     */
    private double calculatePercentage(double ratio) {
        BigDecimal percentage = BigDecimal.valueOf(ratio * 100.0).setScale(2, RoundingMode.HALF_UP);
        return percentage.doubleValue();
    }
}
