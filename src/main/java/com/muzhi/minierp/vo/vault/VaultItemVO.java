package com.muzhi.minierp.vo.vault;

import com.muzhi.minierp.model.vault.VaultProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * {@code VaultItemVO}：已删除详情及轮换内部下载的密文与生命周期信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VaultItemVO extends VaultItemResultVO {

    /**
     * 条目所属代，详情返回
     */
    private Long generation;

    /**
     * 密钥代次
     */
    private Long keyEpoch;

    /**
     * 条目密钥密文盒
     */
    private VaultProtocol.Box wrappedDek;

    /**
     * 完整条目密文盒，服务端不解析密码或 TOTP
     */
    private VaultProtocol.Box encryptedItem;
}
