package com.muzhi.minierp.component.vault;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.muzhi.minierp.config.VaultProperties;
import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.entity.vault.VaultPasskey;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.VaultPasskeyMapper;
import com.muzhi.minierp.model.vault.VaultProtocol;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.vo.vault.VaultOperationOptionsVO;
import com.muzhi.minierp.vo.vault.VaultWebAuthnOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * {@code VaultOperationAuthorization}：账户锁内验证单次删除授权，不提升会话认证状态
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-23
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultOperationAuthorization {

    private final VaultProperties properties;

    private final VaultChallengeStore challenges;

    private final VaultPasskeyMapper credentials;

    private final VaultWebAuthn webAuthn;

    private final VaultAccess access;

    /**
     * 调用方须已在事务中校验完整会话、账户及目标的可写状态。
     */
    public VaultOperationOptionsVO options(VaultSession session, VaultAccount account, VaultInfo vault, VaultEnum.Operation operation, Object request) {
        LambdaQueryWrapper<VaultPasskey> query = Wrappers.lambdaQuery();
        query.eq(VaultPasskey::getAccountId, account.getId());
        query.isNull(VaultPasskey::getRevokedAt);
        List<VaultPasskey> keys = credentials.selectList(query);
        VaultEnum.Error.MFA_REQUIRED.rejectIf(keys.isEmpty());
        byte[] binding = this.binding(session, account, vault, operation, request);
        VaultOperationOptionsVO result = challenges.issueOperation(binding);
        VaultWebAuthnOptionsVO publicKey = result.getPublicKey();
        publicKey.setRpId(properties.getRpId());
        publicKey.setUserVerification(VaultProtocol.REQUIRED);
        publicKey.setCredentialRevision(account.getCredentialRevision());
        List<VaultWebAuthnOptionsVO.CredentialDescriptorVO> allowed = new ArrayList<>();
        for (VaultPasskey key : keys) {
            VaultWebAuthnOptionsVO.CredentialDescriptorVO descriptor = new VaultWebAuthnOptionsVO.CredentialDescriptorVO();
            descriptor.setId(VaultCodec.encode(key.getCredentialId()));
            descriptor.setType(VaultProtocol.CREDENTIAL_TYPE);
            allowed.add(descriptor);
        }
        publicKey.setAllowCredentials(allowed);
        return result;
    }

    /**
     * 只在尚无成功回执的新执行中调用，验证及凭据计数更新均处于删除事务内。
     */
    public void verify(VaultSession session, VaultAccount account, VaultInfo vault, VaultEnum.Operation operation, Object request, VaultProtocol.PasskeyAuthorization authorization) {
        VaultEnum.Error.OPERATION_AUTHORIZATION_INVALID.rejectIf(authorization == null);
        byte[] binding = this.binding(session, account, vault, operation, request);
        byte[] challenge = challenges.consumeOperation(authorization.challengeId(), binding);
        VaultProtocol.Credential credential = authorization.credential();
        VaultProtocolValidator.credential(credential, false);
        byte[] credentialId = VaultCodec.decode(credential.rawId(), 1, 1024);
        LambdaQueryWrapper<VaultPasskey> query = Wrappers.lambdaQuery();
        query.eq(VaultPasskey::getAccountId, account.getId());
        query.eq(VaultPasskey::getCredentialId, credentialId);
        query.isNull(VaultPasskey::getRevokedAt);
        VaultPasskey key = credentials.selectOne(query);
        VaultEnum.Error.PASSKEY_INVALID.rejectIf(key == null);
        webAuthn.authenticate(challenge, account.getWebauthnUserHandle(), key, credential);
        key.setLastUsedAt(OffsetDateTime.now());
        access.touch(key, session.ownerId());
        credentials.updateById(key);
    }

    private byte[] binding(VaultSession session, VaultAccount account, VaultInfo vault, VaultEnum.Operation operation, Object request) {
        List<Object> context = List.of(session.ownerId(), account.getId(), vault.getVaultId(), operation.getCode(), session.bindingId(), session.authEpoch(), account.getCredentialRevision(), request);
        return VaultCodec.digest(context);
    }
}
