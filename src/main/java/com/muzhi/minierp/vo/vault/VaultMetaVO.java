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
