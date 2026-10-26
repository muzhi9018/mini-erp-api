package com.muzhi.minierp.model.vault;

import com.alibaba.fastjson2.annotation.JSONField;
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

    /**
     * WebAuthn 公钥凭据类型。
     */
    public static final String CREDENTIAL_TYPE = "public-key";

    /**
     * WebAuthn 必选策略值，用于要求常驻凭据和用户验证。
     */
    public static final String REQUIRED = "required";

    /**
     * WebAuthn 注册时的证明传送偏好，不要求传送认证器证明。
     */
    public static final String ATTESTATION = "none";

    /**
     * 密文封装使用的加密算法：256 位密钥的 AES-GCM。
     */
    public static final String ALGORITHM = "A256GCM";

    /**
     * 主密码密钥派生参数档案：Argon2id v19，内存 65536 KiB、迭代 3 次、并行度 4、输出 32 字节。
     */
    public static final String KDF_PROFILE = "ARGON2ID_V19_M65536_T3_P4_L32";

    /**
     * 客户端声明密文条目结构版本的请求头名称。
     */
    public static final String CLIENT_SCHEMA_HEADER = "X-Vault-Item-Schema-Version";

    /**
     * 当前支持的密文条目结构版本。
     */
    public static final int ITEM_SCHEMA_VERSION = 1;

    /**
     * 客户端声明条目生命周期协议版本的请求头名称。
     */
    public static final String CLIENT_LIFECYCLE_HEADER = "X-Vault-Lifecycle-Version";

    /**
     * 当前支持的条目生命周期协议版本。
     */
    public static final int LIFECYCLE_PROTOCOL_VERSION = 1;

    /**
     * 已删除密文条目在永久清理前的保留天数。
     */
    public static final int DELETED_ITEM_RETENTION_DAYS = 30;

    /**
     * 版本号上限，取 JavaScript 最大安全整数，避免客户端数值精度丢失。
     */
    public static final long MAX_VERSION = 9007199254740991L;

    private VaultProtocol() { }

    /**
     * 通用密文封装，统一传输加密格式版本、算法、初始化向量和密文，供密钥封装与条目加密复用。
     */
    @JSONType(alphabetic = false)
    public record Box(Integer formatVersion, String algorithm, String iv, String ciphertext) {
    }

    /**
     * 密钥槽传输模型，携带密钥派生参数、盐值及封装后的保险库主密钥。
     * 槽类型参见 {@link com.muzhi.minierp.enums.VaultEnum.SlotType}，区分主密码解锁与恢复密钥解锁。
     */
    @JSONType(alphabetic = false)
    public record Slot(String slotId, String slotType, String keyId, String kdfProfile,
                       String kdfSalt, String wrapSalt, String recoverySalt, Box wrappedVmk) {
    }

    /**
     * 密文条目传输模型，用于读取条目及上传轮换数据，携带版本信息、封装后的数据密钥和条目密文。
     */
    @JSONType(alphabetic = false)
    public record Item(String itemId, Long contentVersion, Long rowVersion, Long keyEpoch,
                       Box wrappedDek, Box encryptedItem) {
    }

    /**
     * 保险库初始化请求，提交保险库标识及主密码、恢复密钥对应的密钥槽，并通过幂等键防止重复初始化。
     */
    @JSONType(alphabetic = false)
    public record Setup(String vaultId, List<Slot> keySlots, String idempotencyKey) {
    }

    /**
     * 单条密文条目的创建、更新或删除请求，通过活动代、预期行版本和幂等键控制写入。
     * 创建与更新时携带密文内容，删除操作使用 Passkey 授权证明。
     */
    @JSONType(alphabetic = false)
    public record ItemWrite(String itemId, Long generation, Long keyEpoch, Long expectedRowVersion,
                            Long contentVersion, Box wrappedDek, Box encryptedItem, String idempotencyKey,
                            @JSONField(serialize = false) PasskeyAuthorization passkeyAuthorization) {

        public ItemWrite(String itemId, Long generation, Long keyEpoch, Long expectedRowVersion,
                         Long contentVersion, Box wrappedDek, Box encryptedItem, String idempotencyKey) {
            this(itemId, generation, keyEpoch, expectedRowVersion, contentVersion, wrappedDek, encryptedItem, idempotencyKey, null);
        }
    }

    /**
     * 条目生命周期操作请求，用于恢复已删除条目、永久删除条目或直接删除账号组。
     * 仅接收目标与预期版本等操作参数，不接收客户端删除元数据或密文。
     */
    @JSONType(alphabetic = false)
    public record ItemLifecycle(String itemId, Long generation, Long expectedRowVersion, String idempotencyKey,
                                @JSONField(serialize = false) PasskeyAuthorization passkeyAuthorization) {

        public ItemLifecycle(String itemId, Long generation, Long expectedRowVersion, String idempotencyKey) {
            this(itemId, generation, expectedRowVersion, idempotencyKey, null);
        }
    }

    /**
     * 原子批量操作中的单条密文变更，描述操作类型、目标条目、预期版本及所需密文。
     * action 参见 {@link com.muzhi.minierp.enums.VaultEnum.ItemChangeAction}。
     */
    @JSONType(alphabetic = false)
    public record ItemChange(String action, String itemId, Long expectedRowVersion,
                             Long contentVersion, Box wrappedDek, Box encryptedItem) {
    }

    /**
     * 同一活动代的原子批量变更请求，通过预期集合版本校验并发修改，统一提交多条条目变更。
     * 不传输组名、成员关系或邮箱明文。
     */
    @JSONType(alphabetic = false)
    public record ItemBatch(Long generation, Long keyEpoch, Long expectedCollectionRevision,
                            List<ItemChange> changes, String idempotencyKey,
                            @JSONField(serialize = false) PasskeyAuthorization passkeyAuthorization) {

        public ItemBatch(Long generation, Long keyEpoch, Long expectedCollectionRevision,
                         List<ItemChange> changes, String idempotencyKey) {
            this(generation, keyEpoch, expectedCollectionRevision, changes, idempotencyKey, null);
        }
    }

    /**
     * 敏感删除操作的 Passkey 授权证明，携带挑战标识和 WebAuthn 凭据。
     * 仅用于传入验证，不参与业务摘要或回执序列化。
     */
    public record PasskeyAuthorization(String challengeId, Credential credential) {
    }

    /**
     * 主密码变更请求，通过替换普通密钥槽更新保险库主密钥的封装，并校验预期元数据版本。
     */
    @JSONType(alphabetic = false)
    public record MasterChange(Long expectedMetaRevision, Slot normalSlot, String idempotencyKey) {
    }

    /**
     * 密钥轮换或备份恢复任务的启动请求，携带目标密钥槽、预期版本及恢复时的条目清单。
     */
    @JSONType(alphabetic = false)
    public record RotationStart(String rotationId, String vaultId, Long expectedMetaRevision,
                                Long expectedCollectionRevision, List<Slot> keySlots, List<String> itemIds,
                                String idempotencyKey) {
    }

    /**
     * 轮换或恢复任务的控制请求，用于提交、恢复执行或取消任务；提交时携带客户端核验后的清单摘要。
     */
    @JSONType(alphabetic = false)
    public record RotationCommand(String rotationId, Long expectedCollectionRevision,
                                  String verifiedManifestDigest, String idempotencyKey) {
    }

    /**
     * 轮换或恢复任务的分批上传请求，通过批次标识提交目标代的条目密文。
     */
    @JSONType(alphabetic = false)
    public record RotationBatch(String rotationId, String batchId, List<Item> items) {
    }

    /**
     * 多因素认证恢复请求，提交一次性身份恢复码以进入受限恢复会话。
     */
    public record RecoveryInput(String code) {
    }

    /**
     * 身份凭据管理请求，用于签发恢复码或撤销 Passkey，并通过预期凭据版本校验并发变更。
     */
    public record CredentialCommand(String credentialId, Long expectedCredentialRevision) {
    }

    /**
     * 身份恢复码确认请求，确认指定批次已保存，并通过预期凭据版本校验并发变更。
     */
    public record RecoveryConfirmation(String batchId, Long expectedCredentialRevision) {
    }

    /**
     * WebAuthn 公钥凭据传输模型，承载凭据标识、类型及客户端响应，用于 Passkey 注册、认证和操作授权。
     */
    public record Credential(String id, String rawId, String type, CredentialResponse response) {
    }

    /**
     * WebAuthn 客户端响应，承载注册证明或认证断言数据，供服务端校验挑战、来源及认证器证明或签名。
     */
    public record CredentialResponse(String clientDataJSON, String attestationObject,
                                     String authenticatorData, String signature, String userHandle,
                                     List<String> transports) {
    }

    /**
     * Passkey 注册请求，提交用户设置的凭据标签及 WebAuthn 注册凭据。
     */
    public record Registration(String label, Credential credential) {
    }
}
