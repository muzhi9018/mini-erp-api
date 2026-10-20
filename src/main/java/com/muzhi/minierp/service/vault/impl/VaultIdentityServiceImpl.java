package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.muzhi.minierp.component.vault.VaultAccess;
import com.muzhi.minierp.component.vault.VaultChallengeStore;
import com.muzhi.minierp.component.vault.VaultCodec;
import com.muzhi.minierp.component.vault.VaultWebAuthn;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.system.SysUserMapper;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.VaultContexts;
import com.muzhi.minierp.service.vault.*;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.vault.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/**
 * <p>
 * {@code VaultIdentityServiceImpl}：密码库认证与受限身份恢复；与 ERP 登录令牌分离
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultIdentityServiceImpl implements IVaultIdentityService {
    private final com.muzhi.minierp.config.VaultProperties properties;

    private final AuthenticationManager authenticationManager;

    private final VaultControlMapper vaultControl;

    private final SysUserMapper sysUserMapper;

    private final VaultAccountMapper accounts;

    private final VaultPasskeyMapper credentials;

    private final VaultAuthRecoveryCodeMapper recoveryCodes;

    private final VaultAccess access;

    private final VaultChallengeStore challenges;

    private final VaultWebAuthn webAuthn;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSession login(LoginQuery query) {
        // 与系统密码重置、禁用和删除使用同一用户行，避免校验旧密码后继承新认证代次。
        Long lockedOwner = vaultControl.lockLoginUser(query.getUsername());
        VaultEnum.Error.INVALID_CREDENTIALS.rejectIf(lockedOwner == null);
        UsernamePasswordAuthenticationToken request = new UsernamePasswordAuthenticationToken(query.getUsername(), query.getPassword());
        Authentication verified = authenticationManager.authenticate(request);
        LoginUser user = (LoginUser) verified.getPrincipal();
        Long ownerId = user.getId();
        VaultEnum.Error.INVALID_CREDENTIALS.rejectIf(!lockedOwner.equals(ownerId));
        vaultControl.ensureAccount(ownerId, IdWorker.getId(), VaultCodec.random(32));
        VaultAccount account = vaultControl.lockAccount(ownerId);
        VaultEnum.Error.SESSION_REVOKED.rejectIf(account == null || Boolean.TRUE.equals(account.getIsDeleted()));
        Instant now = Instant.now();
        VaultSession session = new VaultSession(ownerId, account.getId(), account.getAuthEpoch(), VaultEnum.Stage.PASSWORD, now, now, null, null, UUID.randomUUID(), false);
        access.check(session);
        access.audit(session, null, VaultEnum.AuditAction.LOGIN);
        return session;
    }

    @Override
    public VaultAccount checkSession(VaultSession session) {
        return access.check(session);
    }

    private List<VaultPasskey> activeKeys(Long accountId) {
        LambdaQueryWrapper<VaultPasskey> query = Wrappers.lambdaQuery();
        query.eq(VaultPasskey::getAccountId, accountId);
        query.isNull(VaultPasskey::getRevokedAt);
        return credentials.selectList(query);
    }

    private void registrationAllowed(VaultSession session, VaultAccount account, List<VaultPasskey> keys) {
        if (session.stage() == VaultEnum.Stage.PASSWORD || session.stage() == VaultEnum.Stage.RECOVERY_ENROLLMENT) {
            VaultEnum.Error.MFA_REQUIRED.rejectIf(!keys.isEmpty());
            if (session.stage() == VaultEnum.Stage.PASSWORD) {
                VaultInfo vault = vaultControl.lockVault(account.getId());
                VaultEnum.Error.MFA_REQUIRED.rejectIf(vault != null && vault.getActiveGeneration() != null);
            }
        } else {
            VaultContexts.requireRecentPasskey(session);
        }
        int maximumPasskeys = 10;
        VaultEnum.Error.INVALID_REQUEST.rejectIf(keys.size() >= maximumPasskeys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultWebAuthnOptionsVO options(VaultSession session, VaultEnum.ChallengePurpose purpose) {
        VaultAccount account = access.lock(session);
        List<VaultPasskey> keys = this.activeKeys(account.getId());
        byte[] challenge;
        VaultWebAuthnOptionsVO result = new VaultWebAuthnOptionsVO();
        if (purpose == VaultEnum.ChallengePurpose.REGISTER) {
            this.registrationAllowed(session, account, keys);
            LambdaQueryWrapper<SysUser> userQuery = Wrappers.lambdaQuery();
            userQuery.select(SysUser::getUsername);
            userQuery.eq(SysUser::getId, session.ownerId());
            SysUser owner = sysUserMapper.selectOne(userQuery);
            VaultEnum.Error.SESSION_REVOKED.rejectIf(owner == null);
            String username = owner.getUsername();

            challenge = challenges.issue(session, purpose, account.getCredentialRevision());
            VaultWebAuthnOptionsVO.RpVO rp = new VaultWebAuthnOptionsVO.RpVO();
            rp.setId(properties.getRpId());
            rp.setName(properties.getRpName());
            result.setRp(rp);

            VaultWebAuthnOptionsVO.UserVO user = new VaultWebAuthnOptionsVO.UserVO();
            user.setId(VaultCodec.encode(account.getWebauthnUserHandle()));
            user.setName(username);
            user.setDisplayName(username);
            result.setUser(user);

            List<VaultWebAuthnOptionsVO.CredentialParameterVO> parameters = new ArrayList<>();
            for (COSEAlgorithmIdentifier algorithm : List.of(COSEAlgorithmIdentifier.ES256, COSEAlgorithmIdentifier.RS256, COSEAlgorithmIdentifier.EdDSA)) {
                VaultWebAuthnOptionsVO.CredentialParameterVO parameter = new VaultWebAuthnOptionsVO.CredentialParameterVO();
                parameter.setType(VaultProtocol.CREDENTIAL_TYPE);
                parameter.setAlg(algorithm.getValue());
                parameters.add(parameter);
            }
            result.setPubKeyCredParams(parameters);
            result.setAttestation(VaultProtocol.ATTESTATION);
            VaultWebAuthnOptionsVO.AuthenticatorSelectionVO selection = new VaultWebAuthnOptionsVO.AuthenticatorSelectionVO();
            selection.setResidentKey(VaultProtocol.REQUIRED);
            selection.setUserVerification(VaultProtocol.REQUIRED);
            result.setAuthenticatorSelection(selection);
            List<VaultWebAuthnOptionsVO.CredentialDescriptorVO> excluded = this.descriptors(keys);
            result.setExcludeCredentials(excluded);
        } else {
            VaultEnum.Error.MFA_REQUIRED.rejectIf(keys.isEmpty());
            VaultEnum.Error.MFA_REQUIRED.rejectIf(session.stage() == VaultEnum.Stage.RECOVERY_ENROLLMENT
                    && session.recoveryUntil().isBefore(Instant.now()));
            challenge = challenges.issue(session, purpose, account.getCredentialRevision());
            result.setRpId(properties.getRpId());
            result.setUserVerification(VaultProtocol.REQUIRED);
            List<VaultWebAuthnOptionsVO.CredentialDescriptorVO> allowed = this.descriptors(keys);
            result.setAllowCredentials(allowed);
        }
        result.setChallenge(VaultCodec.encode(challenge));
        result.setTimeout(120000);
        result.setCredentialRevision(account.getCredentialRevision());
        return result;
    }

    private List<VaultWebAuthnOptionsVO.CredentialDescriptorVO> descriptors(List<VaultPasskey> keys) {
        List<VaultWebAuthnOptionsVO.CredentialDescriptorVO> result = new ArrayList<>();
        for (VaultPasskey key : keys) {
            VaultWebAuthnOptionsVO.CredentialDescriptorVO descriptor = new VaultWebAuthnOptionsVO.CredentialDescriptorVO();
            descriptor.setType(VaultProtocol.CREDENTIAL_TYPE);
            descriptor.setId(VaultCodec.encode(key.getCredentialId()));
            result.add(descriptor);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(VaultSession session, VaultProtocol.Registration registration) {
        VaultAccount account = access.lock(session);
        List<VaultPasskey> keys = this.activeKeys(account.getId());
        this.registrationAllowed(session, account, keys);
        byte[] challenge = challenges.consume(session, VaultEnum.ChallengePurpose.REGISTER, account.getCredentialRevision());
        VaultPasskey credential = webAuthn.register(challenge, registration);
        credential.setId(IdWorker.getId());
        credential.setAccountId(account.getId());
        access.create(credential, session.ownerId());
        credentials.insert(credential);
        account.setCredentialRevision(account.getCredentialRevision() + 1);
        access.touch(account, session.ownerId());
        accounts.updateById(account);
        access.audit(session, null, VaultEnum.AuditAction.PASSKEY_REGISTER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSession authenticate(VaultSession session, VaultProtocol.Credential credential) {
        VaultAccount account = access.lock(session);
        byte[] challenge = challenges.consume(session, VaultEnum.ChallengePurpose.AUTHENTICATE, account.getCredentialRevision());
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
        VaultEnum.Stage stage = Boolean.TRUE.equals(account.getRecoveryRequired()) ? VaultEnum.Stage.RECOVERY_SETUP : VaultEnum.Stage.MFA;
        VaultSession next = new VaultSession(session.ownerId(), session.accountId(), session.authEpoch(), stage,
                session.loginAt(), session.passwordAt(), Instant.now(), session.recoveryUntil(), session.bindingId(), session.recovering());
        access.audit(session, null, VaultEnum.AuditAction.PASSKEY_AUTHENTICATE);
        return next;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRecoveryCodesVO issueRecoveryCodes(VaultSession session, Long expectedCredentialRevision) {
        VaultContexts.requireRecentPasskey(session);
        VaultEnum.Error.MFA_REQUIRED.rejectIf(session.stage() != VaultEnum.Stage.MFA && session.stage() != VaultEnum.Stage.RECOVERY_SETUP);
        VaultAccount account = access.lock(session);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!account.getCredentialRevision().equals(expectedCredentialRevision));
        LambdaQueryWrapper<VaultAuthRecoveryCode> pending = Wrappers.lambdaQuery();
        pending.eq(VaultAuthRecoveryCode::getAccountId, account.getId());
        pending.isNull(VaultAuthRecoveryCode::getConfirmedAt);
        pending.isNull(VaultAuthRecoveryCode::getRevokedAt);
        List<VaultAuthRecoveryCode> oldPending = recoveryCodes.selectList(pending);
        OffsetDateTime now = OffsetDateTime.now();
        for (VaultAuthRecoveryCode code : oldPending) {
            code.setRevokedAt(now);
            access.touch(code, session.ownerId());
            recoveryCodes.updateById(code);
        }
        UUID batchId = UUID.randomUUID();
        List<String> values = new ArrayList<>();
        int recoveryCodeCount = 8;
        for (int index = 0; index < recoveryCodeCount; index++) {
            byte[] raw = VaultCodec.random(32);
            VaultAuthRecoveryCode code = new VaultAuthRecoveryCode();
            code.setId(IdWorker.getId());
            code.setAccountId(account.getId());
            code.setBatchId(batchId);
            code.setCodeDigest(VaultCodec.sha256(raw));
            access.create(code, session.ownerId());
            recoveryCodes.insert(code);
            values.add(VaultCodec.encode(raw));
            Arrays.fill(raw, (byte) 0);
        }
        access.audit(session, null, VaultEnum.AuditAction.RECOVERY_CODES_ISSUE);
        VaultRecoveryCodesVO result = new VaultRecoveryCodesVO();
        result.setFormatVersion(1);
        result.setBatchId(batchId.toString());
        result.setOwnerSubject(session.ownerId().toString());
        result.setCodes(values);
        result.setCredentialRevision(account.getCredentialRevision());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSession confirmRecoveryCodes(VaultSession session, VaultProtocol.RecoveryConfirmation confirmation) {
        VaultContexts.requireRecentPasskey(session);
        VaultEnum.Error.MFA_REQUIRED.rejectIf(session.stage() != VaultEnum.Stage.MFA && session.stage() != VaultEnum.Stage.RECOVERY_SETUP);
        VaultAccount account = access.lock(session);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!account.getCredentialRevision().equals(confirmation.expectedCredentialRevision()));
        UUID batchId = VaultCodec.uuid(confirmation.batchId());
        LambdaQueryWrapper<VaultAuthRecoveryCode> query = Wrappers.lambdaQuery();
        query.eq(VaultAuthRecoveryCode::getAccountId, account.getId());
        List<VaultAuthRecoveryCode> codes = recoveryCodes.selectList(query);
        List<VaultAuthRecoveryCode> batch = codes.stream().filter(code -> code.getBatchId().equals(batchId)).toList();
        OffsetDateTime now = OffsetDateTime.now();
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(batch.size() != 8 || batch.stream().anyMatch(code -> code.getRevokedAt() != null
                || code.getConsumedAt() != null || code.getGmtCreate().isBefore(now.minusMinutes(10))));
        for (VaultAuthRecoveryCode code : codes) {
            if (code.getBatchId().equals(batchId)) {
                code.setConfirmedAt(now);
            } else if (code.getRevokedAt() == null) {
                code.setRevokedAt(now);
            }
            access.touch(code, session.ownerId());
            recoveryCodes.updateById(code);
        }
        account.setActiveRecoveryBatchId(batchId);
        account.setRecoveryRequired(false);
        account.setCredentialRevision(account.getCredentialRevision() + 1);
        access.touch(account, session.ownerId());
        accounts.updateById(account);
        if (session.recovering()) {
            access.revoke(account, session.ownerId());
        }
        access.audit(session, null, VaultEnum.AuditAction.RECOVERY_CODES_CONFIRM);
        return new VaultSession(session.ownerId(), session.accountId(), account.getAuthEpoch(), VaultEnum.Stage.MFA,
                session.loginAt(), session.passwordAt(), session.mfaAt(), null, session.recovering() ? UUID.randomUUID() : session.bindingId(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSession recover(VaultSession session, String value) {
        VaultEnum.Error.MFA_REQUIRED.rejectIf(session.stage() != VaultEnum.Stage.PASSWORD
                || session.passwordAt().isBefore(Instant.now().minusSeconds(300)));
        VaultAccount account = access.lock(session);
        byte[] raw = VaultCodec.decode(value, 32, 32);
        byte[] digest = VaultCodec.sha256(raw);
        Arrays.fill(raw, (byte) 0);
        LambdaQueryWrapper<VaultAuthRecoveryCode> query = Wrappers.lambdaQuery();
        query.eq(VaultAuthRecoveryCode::getAccountId, account.getId());
        query.eq(VaultAuthRecoveryCode::getBatchId, account.getActiveRecoveryBatchId());
        query.eq(VaultAuthRecoveryCode::getCodeDigest, digest);
        query.isNotNull(VaultAuthRecoveryCode::getConfirmedAt);
        query.isNull(VaultAuthRecoveryCode::getConsumedAt);
        query.isNull(VaultAuthRecoveryCode::getRevokedAt);
        VaultAuthRecoveryCode code = account.getActiveRecoveryBatchId() == null ? null : recoveryCodes.selectOne(query);
        VaultEnum.Error.MFA_RECOVERY_INVALID.rejectIf(code == null);
        code.setConsumedAt(OffsetDateTime.now());
        access.touch(code, session.ownerId());
        recoveryCodes.updateById(code);
        List<VaultPasskey> keys = this.activeKeys(account.getId());
        for (VaultPasskey key : keys) {
            key.setRevokedAt(OffsetDateTime.now());
            access.touch(key, session.ownerId());
            credentials.updateById(key);
        }
        account.setCredentialRevision(account.getCredentialRevision() + 1);
        account.setRecoveryRequired(true);
        access.revoke(account, session.ownerId());
        access.audit(session, null, VaultEnum.AuditAction.MFA_RECOVERY);
        Instant now = Instant.now();
        return new VaultSession(session.ownerId(), session.accountId(), account.getAuthEpoch(), VaultEnum.Stage.RECOVERY_ENROLLMENT,
                now, session.passwordAt(), null, now.plusSeconds(600), UUID.randomUUID(), true);
    }

    @Override
    public List<VaultPasskeyVO> passkeys(VaultSession session) {
        VaultContexts.requireMfa(session);
        access.check(session);
        List<VaultPasskey> keys = this.activeKeys(session.accountId());
        List<VaultPasskeyVO> result = BeanConvertUtils.mapList(keys, VaultPasskeyVO.class);
        for (int index = 0; index < keys.size(); index++) {
            VaultPasskey key = keys.get(index);
            VaultPasskeyVO view = result.get(index);
            view.setCredentialId(VaultCodec.encode(key.getCredentialId()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePasskey(VaultSession session, VaultProtocol.CredentialCommand command) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!account.getCredentialRevision().equals(command.expectedCredentialRevision()));
        List<VaultPasskey> keys = this.activeKeys(account.getId());
        byte[] id = VaultCodec.decode(command.credentialId(), 1, 1024);
        VaultPasskey found = keys.stream().filter(key -> Arrays.equals(id, key.getCredentialId())).findFirst().orElse(null);
        VaultEnum.Error.NOT_FOUND.rejectIf(found == null);
        VaultEnum.Error.LAST_PASSKEY.rejectIf(keys.size() <= 1);
        found.setRevokedAt(OffsetDateTime.now());
        access.touch(found, session.ownerId());
        credentials.updateById(found);
        account.setCredentialRevision(account.getCredentialRevision() + 1);
        access.revoke(account, session.ownerId());
        access.audit(session, null, VaultEnum.AuditAction.PASSKEY_REVOKE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(VaultSession session) {
        access.audit(session, null, VaultEnum.AuditAction.LOGOUT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeOwner(Long ownerId) {
        VaultAccount account = vaultControl.lockAccount(ownerId);
        if (account != null) {
            access.revoke(account, null);
        }
    }

}
