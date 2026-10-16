package com.muzhi.minierp.model.vault;

import java.util.List;

/**
 * <p>
 * {@code VaultProtocol}：客户端密文协议的传输模型；不接收数据库审计或归属字段
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public final class VaultProtocol {
    public static final String CREDENTIAL_TYPE = "public-key";
    public static final String REQUIRED = "required";
    public static final String ATTESTATION = "none";
    public static final String ALGORITHM = "A256GCM";
    public static final String KDF_PROFILE = "ARGON2ID_V19_M65536_T3_P4_L32";
    public static final long MAX_VERSION = 9007199254740991L;

    private VaultProtocol() { }

    public record Box(Integer formatVersion, String algorithm, String iv, String ciphertext) { }

    public record Slot(String slotId, String slotType, String keyId, String kdfProfile,
            String kdfSalt, String wrapSalt, String recoverySalt, Box wrappedVmk) { }

    public record Item(String itemId, Long contentVersion, Long rowVersion, Long keyEpoch,
            Box wrappedDek, Box encryptedItem) { }

    public record Setup(String vaultId, List<Slot> keySlots, String idempotencyKey) { }

    public record ItemWrite(String itemId, Long generation, Long keyEpoch, Long expectedRowVersion,
            Long contentVersion, Box wrappedDek, Box encryptedItem, String idempotencyKey) { }

    public record MasterChange(Long expectedMetaRevision, Slot normalSlot, String idempotencyKey) { }

    public record RotationStart(String rotationId, String vaultId, Long expectedMetaRevision,
            Long expectedCollectionRevision, List<Slot> keySlots, List<String> itemIds, String idempotencyKey) { }

    public record RotationCommand(String rotationId, Long expectedCollectionRevision,
            String verifiedManifestDigest, String idempotencyKey) { }

    public record RotationBatch(String rotationId, String batchId, List<Item> items) { }

    public record RecoveryInput(String code) { }

    public record CredentialCommand(String credentialId, Long expectedCredentialRevision) { }

    public record RecoveryConfirmation(String batchId, Long expectedCredentialRevision) { }

    public record Credential(String id, String rawId, String type, CredentialResponse response) { }

    public record CredentialResponse(String clientDataJSON, String attestationObject,
            String authenticatorData, String signature, String userHandle, List<String> transports) { }

    public record Registration(String label, Credential credential) { }
}
