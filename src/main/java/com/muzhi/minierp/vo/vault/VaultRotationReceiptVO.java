package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultRotationReceiptVO}：轮换原子提交后持久化的公开回执
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultRotationReceiptVO {

    /**
     * 公开任务标识
     */
    private String rotationId;

    /**
     * 提交后的活动代
     */
    private Long generation;

    /**
     * 提交后的密钥代次
     */
    private Long keyEpoch;

    /**
     * 提交后的元数据版本
     */
    private Long metaRevision;

    /**
     * 提交后的集合版本
     */
    private Long collectionRevision;
}
