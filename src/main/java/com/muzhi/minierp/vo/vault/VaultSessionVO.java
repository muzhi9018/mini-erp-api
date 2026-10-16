package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultSessionVO}：浏览器会话的公开状态，不包含会话标识与认证秘密
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultSessionVO {

    /**
     * 登录方式
     */
    private String loginMethod;

    /**
     * 当前 CSRF 令牌
     */
    private String csrfToken;

    /**
     * CSRF 请求头名称
     */
    private String csrfHeader;

    /**
     * 当前用户的公开标识
     */
    private String ownerSubject;

    /**
     * 认证阶段，参见 {@link com.muzhi.minierp.enums.VaultEnum.Stage}
     */
    private String stage;

    /**
     * 是否已经完成多因素认证
     */
    private Boolean mfa;

    /**
     * 是否在近期完成认证
     */
    private Boolean recentAuthentication;

    /**
     * 凭据集合版本
     */
    private Long credentialRevision;

    /**
     * 是否需要配置恢复码
     */
    private Boolean recoveryRequired;

    /**
     * 再次认证的挑战获取地址
     */
    private String optionsEndpoint;
}
