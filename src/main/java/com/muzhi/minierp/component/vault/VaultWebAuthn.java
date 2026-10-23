package com.muzhi.minierp.component.vault;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.muzhi.minierp.config.VaultProperties;
import com.muzhi.minierp.entity.vault.VaultPasskey;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultProtocol;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.credential.CredentialRecordImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.security.MessageDigest;
import java.util.*;

/**
 * <p>
 * {@code VaultWebAuthn}：使用 Spring WebAuthn 依赖的 WebAuthn4J 验证库，不自行实现签名算法
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultWebAuthn {

    private final VaultProperties properties;

    private final WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();

    private final ObjectConverter converter = new ObjectConverter();

    public VaultPasskey register(byte[] challenge, VaultProtocol.Registration registration) {
        try {
            VaultProtocol.Credential credential = registration.credential();
            VaultProtocol.CredentialResponse response = credential.response();
            byte[] client = VaultCodec.decode(response.clientDataJSON(), 1, 8192);
            this.checkClient(client);
            byte[] attestation = VaultCodec.decode(response.attestationObject(), 1, 16384);
            RegistrationRequest request = new RegistrationRequest(attestation, client, Set.of());
            List<PublicKeyCredentialParameters> algorithms = List.of(
                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256),
                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.EdDSA));
            ServerProperty server = this.server(challenge);
            RegistrationParameters parameters = new RegistrationParameters(server, algorithms, true, true);
            RegistrationData verified = manager.verify(request, parameters);
            AttestationObject attestationObject = verified.getAttestationObject();
            AuthenticatorData<RegistrationExtensionAuthenticatorOutput> auth = attestationObject.getAuthenticatorData();
            AttestedCredentialData data = auth.getAttestedCredentialData();
            byte[] credentialId = VaultCodec.decode(credential.rawId(), 1, 1024);
            VaultEnum.Error.PASSKEY_INVALID.rejectIf(data == null || !MessageDigest.isEqual(data.getCredentialId(), credentialId));
            VaultPasskey key = new VaultPasskey();
            key.setCredentialId(credentialId);
            CBORMapper cborMapper = converter.getCborMapper();
            byte[] publicKey = cborMapper.writeValueAsBytes(data.getCOSEKey());
            VaultEnum.Error.PASSKEY_INVALID.rejectIf(publicKey.length > 4096);
            key.setPublicKeyCose(publicKey);
            key.setAaguid(data.getAaguid().getValue());
            key.setSignatureCount(auth.getSignCount());
            key.setBackupEligible(auth.isFlagBE());
            key.setBackupState(auth.isFlagBS());
            key.setTransports(new String[0]);
            key.setLabel(registration.label());
            return key;
        } catch (RuntimeException failure) {
            throw VaultEnum.Error.PASSKEY_INVALID.exception();
        }
    }

    public void authenticate(byte[] challenge, byte[] userHandle, VaultPasskey key, VaultProtocol.Credential credential) {
        try {
            VaultProtocol.CredentialResponse response = credential.response();
            byte[] client = VaultCodec.decode(response.clientDataJSON(), 1, 8192);
            this.checkClient(client);
            if (response.userHandle() != null) {
                byte[] suppliedHandle = VaultCodec.decode(response.userHandle(), 32, 32);
                VaultEnum.Error.PASSKEY_INVALID.rejectIf(!MessageDigest.isEqual(userHandle, suppliedHandle));
            }
            COSEKey cose = converter.getCborMapper().readValue(key.getPublicKeyCose(), COSEKey.class);
            AttestedCredentialData attested = new AttestedCredentialData(new AAGUID(key.getAaguid()), key.getCredentialId(), cose);
            CredentialRecordImpl record = new CredentialRecordImpl(null, true, key.getBackupEligible(), key.getBackupState(),
                    key.getSignatureCount(), attested, null, null, null, Set.of());
            byte[] authData = VaultCodec.decode(response.authenticatorData(), 37, 16384);
            byte[] signature = VaultCodec.decode(response.signature(), 1, 4096);
            AuthenticationRequest request = new AuthenticationRequest(key.getCredentialId(), authData, client, signature);
            ServerProperty server = this.server(challenge);
            AuthenticationParameters parameters = new AuthenticationParameters(server, record, List.of(key.getCredentialId()), true, true);
            AuthenticationData verified = manager.verify(request, parameters);
            AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> result = verified.getAuthenticatorData();
            key.setSignatureCount(result.getSignCount());
            key.setBackupState(result.isFlagBS());
        } catch (RuntimeException failure) {
            throw VaultEnum.Error.PASSKEY_INVALID.exception();
        }
    }

    private ServerProperty server(byte[] challenge) {
        return new ServerProperty(Set.of(new Origin(properties.getOrigin())), properties.getRpId(), new DefaultChallenge(challenge));
    }

    private void checkClient(byte[] client) {
        JSONObject data = JSON.parseObject(client);
        VaultEnum.Error.PASSKEY_INVALID.rejectIf(data == null || data.getBooleanValue("crossOrigin")
                || !properties.getOrigin().equals(data.getString("origin")));
    }
}
