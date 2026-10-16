package com.muzhi.minierp.vo.vault;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * {@code VaultWebAuthnOptionsVO}：Passkey 注册与认证共用的挑战参数
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultWebAuthnOptionsVO {

    /**
     * 采用无填充 Base64url 编码的单次挑战
     */
    private String challenge;

    /**
     * 挑战有效时间，单位毫秒
     */
    private Integer timeout;

    /**
     * 签发挑战时的凭据集合版本
     */
    private Long credentialRevision;

    /**
     * 注册时的依赖方信息
     */
    private RpVO rp;

    /**
     * 注册时的用户信息
     */
    private UserVO user;

    /**
     * 注册时允许的公钥算法
     */
    private List<CredentialParameterVO> pubKeyCredParams;

    /**
     * 注册时的证明策略
     */
    private String attestation;

    /**
     * 注册时的认证器选择要求
     */
    private AuthenticatorSelectionVO authenticatorSelection;

    /**
     * 注册时排除的已有凭据
     */
    private List<CredentialDescriptorVO> excludeCredentials;

    /**
     * 认证时的依赖方标识
     */
    private String rpId;

    /**
     * 认证时的用户验证要求
     */
    private String userVerification;

    /**
     * 认证时允许使用的凭据
     */
    private List<CredentialDescriptorVO> allowCredentials;

    /**
     * <p>
     * {@code RpVO}：WebAuthn 依赖方信息
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class RpVO {

        /**
         * 依赖方域名
         */
        private String id;

        /**
         * 依赖方显示名称
         */
        private String name;
    }

    /**
     * <p>
     * {@code UserVO}：WebAuthn 用户信息
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class UserVO {

        /**
         * 经过 Base64url 编码的 WebAuthn 用户句柄
         */
        private String id;

        /**
         * 用户名称
         */
        private String name;

        /**
         * 用户显示名称
         */
        private String displayName;
    }

    /**
     * <p>
     * {@code CredentialParameterVO}：允许的公钥凭据算法
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class CredentialParameterVO {

        /**
         * WebAuthn 凭据类型
         */
        private String type;

        /**
         * COSE 算法标识
         */
        private Long alg;
    }

    /**
     * <p>
     * {@code AuthenticatorSelectionVO}：认证器选择要求
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class AuthenticatorSelectionVO {

        /**
         * 可发现凭据要求
         */
        private String residentKey;

        /**
         * 用户验证要求
         */
        private String userVerification;
    }

    /**
     * <p>
     * {@code CredentialDescriptorVO}：公开凭据描述
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class CredentialDescriptorVO {

        /**
         * WebAuthn 凭据类型
         */
        private String type;

        /**
         * 采用 Base64url 编码的凭据标识
         */
        private String id;
    }
}
