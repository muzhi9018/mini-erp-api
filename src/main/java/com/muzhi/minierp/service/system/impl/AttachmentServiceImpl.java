package com.muzhi.minierp.service.system.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.muzhi.minierp.config.OssConfigProperties;
import com.muzhi.minierp.entity.system.Attachment;
import com.muzhi.minierp.enums.OssModelEnum;
import com.muzhi.minierp.enums.AttachmentEnum;
import com.muzhi.minierp.model.ObjectUploaded;
import com.muzhi.minierp.mapper.system.AttachmentMapper;
import com.muzhi.minierp.service.oss.IOssBridgeManager;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 附件信息表，用于记录上传到对象存储中的文件信息 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements IAttachmentService {

    private final IOssBridgeManager ossBridgeManager;

    private final TransactionTemplate transactionTemplate;

    private final OssConfigProperties ossConfigProperties;

    @Override
    public void validateAttachments(Collection<Long> attachmentIds) {
        Set<Long> ids = this.distinctIds(attachmentIds);
        List<Attachment> attachments = this.findAttachments(ids);
        Assert.isTrue(attachments.size() != ids.size(), "system.attachment.not-found", "附件不存在或已删除");
    }

    @Override
    public Map<Long, String> getAuthorizedUrls(Collection<Long> attachmentIds) {
        Set<Long> ids = this.distinctIds(attachmentIds);
        List<Attachment> attachments = this.findAttachments(ids);
        Map<Long, String> urls = new HashMap<>();
        for (Attachment attachment : attachments) {
            String url = ossBridgeManager.getAuthorizedDownloadUrl(attachment.getBucketName(), attachment.getObjectKey());
            urls.put(attachment.getId(), url);
        }
        return urls;
    }

    private Set<Long> distinctIds(Collection<Long> attachmentIds) {
        Set<Long> ids = new HashSet<>();
        if (attachmentIds != null) {
            ids.addAll(attachmentIds);
            ids.remove(null);
        }
        return ids;
    }

    private List<Attachment> findAttachments(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Attachment> query = Wrappers.lambdaQuery();
        query.in(Attachment::getId, ids);
        return super.baseMapper.selectList(query);
    }

    @Override
    public Attachment upload(MultipartFile file, String model) {
        OssModelEnum ossModelEnum = OssModelEnum.ofModel(model);
        Assert.isNull(ossModelEnum, "system.attachment.model-invalid", "没有找到对应的模块");
        long id = IdWorker.getId();
        String objectKey = ossModelEnum.builderOssKey(id, file.getOriginalFilename());
        String bucketName = ossConfigProperties.getBucketName();
        Assert.isEmpty(bucketName, "system.attachment.bucket-required", "未配置附件存储桶");

        // 确认对象上传完成后再入库，避免保存尚不可用的附件。
        ObjectUploaded uploaded = ossBridgeManager.upload(file, bucketName, objectKey, id);
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setStorageType(AttachmentEnum.StorageType.RUSTFS.getCode());
        attachment.setBucketName(bucketName);
        attachment.setObjectKey(objectKey);
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setEtag(uploaded.getEtag());
        attachment.setAccessType(AttachmentEnum.AccessType.PRIVATE.getCode());
        attachment.setIsDeleted(false);

        try {
            // 数据库事务仅覆盖入库阶段，避免网络上传期间占用连接。
            transactionTemplate.executeWithoutResult(status -> {
                int inserted = super.baseMapper.insert(attachment);
                Assert.isTrue(inserted != 1, "system.attachment.save-failed", "保存附件记录失败");
            });
        } catch (RuntimeException e) {
            // 数据库回滚无法撤销对象上传，需补偿删除本次生成的对象。
            try {
                ossBridgeManager.deleteObject(bucketName, objectKey);
            } catch (RuntimeException cleanupException) {
                e.addSuppressed(cleanupException);
                log.error("附件入库失败后清理对象失败，bucket: {}, key: {}", bucketName, objectKey, cleanupException);
            }
            throw e;
        }
        return attachment;
    }
}
