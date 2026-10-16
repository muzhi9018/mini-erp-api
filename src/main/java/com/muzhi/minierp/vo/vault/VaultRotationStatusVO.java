package com.muzhi.minierp.vo.vault;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.muzhi.minierp.model.vault.VaultProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * {@code VaultRotationStatusVO}：轮换任务详情与持久化提交回执
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VaultRotationStatusVO extends VaultRotationVO {

    /**
     * 任务模式，参见 {@link com.muzhi.minierp.enums.VaultEnum.RotationMode}
     */
    private String mode;

    /**
     * 任务登记的预期条目与目标内容版本
     */
    private List<ExpectedItemVO> expectedItems;

    /**
     * 目标代的密文密钥槽
     */
    private List<VaultProtocol.Slot> keySlots;

    /**
     * 已经接收的批次标识
     */
    private List<String> receivedBatchIds;

    /**
     * 持久化提交回执，未提交时明确返回 null
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private VaultRotationReceiptVO commitReceipt;

    /**
     * <p>
     * {@code ExpectedItemVO}：任务预期条目的公开版本
     * </p>
     *
     * @author Mr.Muzhi
     * @since 2026-10-16
     */
    @Data
    public static class ExpectedItemVO {

        /**
         * 条目标识
         */
        private String itemId;

        /**
         * 目标内容版本
         */
        private Long contentVersion;
    }
}
