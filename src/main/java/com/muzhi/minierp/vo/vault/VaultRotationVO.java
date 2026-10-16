package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultRotationVO}：轮换与恢复任务共用的公开摘要，不暴露实体的内部归属和会话绑定
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultRotationVO {

    /**
     * 公开任务标识
     */
    private String rotationId;

    /**
     * 任务状态，参见 {@link com.muzhi.minierp.enums.VaultEnum.RotationStatus}
     */
    private String status;

    /**
     * 目标代
     */
    private Long generation;

    /**
     * 目标密钥代次
     */
    private Long keyEpoch;

    /**
     * 开始任务时锁定的集合版本
     */
    private Long expectedCollectionRevision;

    /**
     * 预期条目数量
     */
    private Integer expectedItemCount;

    /**
     * 任务绝对截止时间，采用 ISO 8601 格式
     */
    private String expiresAt;
}
