package com.muzhi.minierp.vo.vault;

import com.muzhi.minierp.model.vault.VaultProtocol;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * {@code VaultMetaVO}：密码库的公开元数据，不继承包含内部归属和审计字段的实体
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultMetaVO {

    /**
     * 允许的密文盒格式版本，与明文内容版本及编辑次数独立
     */
    private List<Integer> allowedFormatVersions = List.of(1);

    /**
     * 允许的认证加密算法
     */
    private List<String> allowedAlgorithms = List.of(VaultProtocol.ALGORITHM);

    /**
     * 允许的正常密钥槽派生档案；不接受客户端自定义参数或降级
     */
    private List<String> allowedKdfProfiles = List.of(VaultProtocol.KDF_PROFILE);

    /**
     * 客户端需要兼容读取的条目明文版本；服务端不解析条目明文
     */
    private List<Integer> readableItemSchemaVersions = List.of(1, 2, VaultProtocol.ITEM_SCHEMA_VERSION);

    /**
     * 新保存条目的明文版本
     */
    private Integer writeItemSchemaVersion = VaultProtocol.ITEM_SCHEMA_VERSION;

    /**
     * 数据写请求必须声明的内容兼容能力头
     */
    private String clientSchemaVersionHeader = VaultProtocol.CLIENT_SCHEMA_HEADER;

    /**
     * 最低写入客户端内容版本；当前只接受 writeItemSchemaVersion
     */
    private Integer minimumClientSchemaVersion = VaultProtocol.ITEM_SCHEMA_VERSION;

    /**
     * 是否已经初始化活动代
     */
    private boolean initialized;

    /**
     * 公开密码库标识
     */
    private String vaultId;

    /**
     * 密码库状态，参见 {@link com.muzhi.minierp.enums.VaultEnum.VaultStatus}
     */
    private String status;

    /**
     * 当前活动代
     */
    private Long generation;

    /**
     * 元数据版本
     */
    private Long metaRevision;

    /**
     * 条目与密钥槽集合版本
     */
    private Long collectionRevision;

    /**
     * 当前密钥代次
     */
    private Long keyEpoch;

    /**
     * 活动代的密文密钥槽
     */
    private List<VaultProtocol.Slot> keySlots;
}
