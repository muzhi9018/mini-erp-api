package com.muzhi.minierp.vo.vault;

import lombok.Data;

import java.time.Instant;

/**
 * <p>
 * {@code VaultOperationOptionsVO}：绑定单次删除请求的 Passkey 挑战
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-23
 */
@Data
public class VaultOperationOptionsVO {

    /**
     * 单次操作挑战标识
     */
    private String challengeId;

    /**
     * 浏览器认证所需的公开参数
     */
    private VaultWebAuthnOptionsVO publicKey;

    /**
     * 挑战截止时间
     */
    private Instant expiresAt;
}
