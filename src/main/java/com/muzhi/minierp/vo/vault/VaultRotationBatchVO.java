package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultRotationBatchVO}：轮换暂存批次的接收回执
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultRotationBatchVO {

    /**
     * 批次标识
     */
    private String batchId;

    /**
     * 本批次接受的条目数量
     */
    private Integer accepted;
}
