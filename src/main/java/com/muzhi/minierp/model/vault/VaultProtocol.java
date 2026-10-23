package com.muzhi.minierp.model.vault;

import com.alibaba.fastjson2.annotation.JSONType;

import java.util.List;

/**
 * <p>
 * {@code VaultProtocol}：客户端密文协议的传输模型；不接收数据库审计或归属字段
 * 参与请求摘要的记录保留字段声明顺序，以兼容历史幂等摘要。
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
    public static final String CLIENT_SCHEMA_HEADER = "X-Vault-Item-Schema-Version";
    public static final int ITEM_SCHEMA_VERSION = 1;
    public static final String CLIENT_LIFECYCLE_HEADER = "X-Vault-Lifecycle-Version";
    public static final int LIFECYCLE_PROTOCOL_VERSION = 1;
    public static final int DELETED_ITEM_RETENTION_DAYS = 30;
    public static final long MAX_VERSION = 9007199254740991L;

    private VaultProtocol() {
    }

    @JSONType(alphabetic = false)
    public record Box(Integer formatVersion, String algorithm, String iv, String ciphertext) {
    }

    @JSONType(alphabetic = false)
    public record Slot(String slotId, String slotType, String keyId, String kdfProfile,
                       String kdfSalt, String wrapSalt, String recoverySalt, Box wrappedVmk) {
    }

    @JSONType(alphabetic = false)
    public record Item(String itemId, Long contentVersion, Long rowVersion, Long keyEpoch,
                       Box wrappedDek, Box encryptedItem) {
    }

    @JSONType(alphabetic = false)
    public record Setup(String vaultId, List<Slot> keySlots, String idempotencyKey) {
    }

    @JSONType(alphabetic = false)
    public record ItemWrite(String itemId, Long generation, Long keyEpoch, Long expectedRowVersion,
                            Long contentVersion, Box wrappedDek, Box encryptedItem, String idempotencyKey) {
    }

    /**
     * 恢复、永久删除及直接删除账号组仅接收目标与预期版本，不接收客户端删除元数据或密文。
     */
    @JSONType(alphabetic = false)
    public record ItemLifecycle(String itemId, Long generation, Long expectedRowVersion, String idempotencyKey) {
    }

    /**
     * 单条密文变更；action 参见 {@link com.muzhi.minierp.enums.VaultEnum.ItemChangeAction}。
     */
    @JSONType(alphabetic = false)
    public record ItemChange(String action, String itemId, Long expectedRowVersion,
                             Long contentVersion, Box wrappedDek, Box encryptedItem) {
    }

    /**
     * 同一活动代的原子变更，不传输组名、成员关系或邮箱明文。
     */
    @JSONType(alphabetic = false)
    public record ItemBatch(Long generation, Long keyEpoch, Long expectedCollectionRevision,
                            List<ItemChange> changes, String idempotencyKey) {
    }

    @JSONType(alphabetic = false)
    public record MasterChange(Long expectedMetaRevision, Slot normalSlot, String idempotencyKey) {
    }

    @JSONType(alphabetic = false)
    public record RotationStart(String rotationId, String vaultId, Long expectedMetaRevision,
                                Long expectedCollectionRevision, List<Slot> keySlots, List<String> itemIds,
                                String idempotencyKey) {
    }

    @JSONType(alphabetic = false)
    public record RotationCommand(String rotationId, Long expectedCollectionRevision,
                                  String verifiedManifestDigest, String idempotencyKey) {
    }

    @JSONType(alphabetic = false)
    public record RotationBatch(String rotationId, String batchId, List<Item> items) {
    }

    public record RecoveryInput(String code) {
    }

    public record CredentialCommand(String credentialId, Long expectedCredentialRevision) {
    }

    public record RecoveryConfirmation(String batchId, Long expectedCredentialRevision) {
    }

    public record Credential(String id, String rawId, String type, CredentialResponse response) {
    }

    public record CredentialResponse(String clientDataJSON, String attestationObject,
                                     String authenticatorData, String signature, String userHandle,
                                     List<String> transports) {
    }

    public record Registration(String label, Credential credential) {
    }
}
