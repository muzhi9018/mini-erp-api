package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.vo.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import java.util.*;

/**
 * <p>
 * {@code IVaultIdentityService}：密码库独立认证、凭据恢复与会话撤销
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultIdentityService {

    /**
     * 校验系统密码，只建立待 MFA 身份。
     */
    VaultSession login(LoginQuery query);

    /**
     * 每次请求校验账户状态和数据库认证代次。
     */
    VaultAccount checkSession(VaultSession session);

    /**
     * 为当前身份生成单次、两分钟有效的 WebAuthn 挑战。
     */
    VaultWebAuthnOptionsVO options(VaultSession session, VaultEnum.ChallengePurpose purpose);

    /**
     * 校验注册结果，注册本身不提升 MFA 权限。
     */
    void register(VaultSession session, VaultProtocol.Registration registration);

    /**
     * 验证 Passkey，返回完整认证或恢复码引导身份。
     */
    VaultSession authenticate(VaultSession session, VaultProtocol.Credential credential);

    /**
     * 签发仅返回一次明文的新恢复码批次。
     */
    VaultRecoveryCodesVO issueRecoveryCodes(VaultSession session, Long expectedCredentialRevision);

    /**
     * 确认新恢复码并在身份恢复场景完成新会话交接。
     */
    VaultSession confirmRecoveryCodes(VaultSession session, VaultProtocol.RecoveryConfirmation confirmation);

    /**
     * 原子消费恢复码，仅允许在受限身份中重新绑定 Passkey。
     */
    VaultSession recover(VaultSession session, String code);

    /**
     * 查看当前账户的凭据摘要。
     */
    List<VaultPasskeyVO> passkeys(VaultSession session);

    /**
     * 近期 MFA 后撤销凭据，同时撤销旧会话。
     */
    void revokePasskey(VaultSession session, VaultProtocol.CredentialCommand command);

    /**
     * 记录已成功结束的浏览器会话，不保存会话 ID。
     */
    void logout(VaultSession session);

    /**
     * 系统密码重置、禁用、删除用户时，在原事务内撤销密码库会话。
     */
    void revokeOwner(Long ownerId);
}
