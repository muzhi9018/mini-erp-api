package com.muzhi.minierp.vo.vault;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * {@code VaultRecoveryCodesVO}：仅在签发响应中返回一次明文的恢复码批次
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultRecoveryCodesVO {

    /**
     * 恢复码文件格式版本
     */
    private Integer formatVersion;

    /**
     * 恢复码批次标识
     */
    private String batchId;

    /**
     * 当前用户的公开标识
     */
    private String ownerSubject;

    /**
     * 仅本次签发可读取的明文恢复码
     */
    private List<String> codes;

    /**
     * 签发时的凭据集合版本
     */
    private Long credentialRevision;
}
