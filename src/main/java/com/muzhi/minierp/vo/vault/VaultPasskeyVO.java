package com.muzhi.minierp.vo.vault;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * <p>
 * {@code VaultPasskeyVO}：凭据摘要，仅暴露管理界面所需字段
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultPasskeyVO {

    /**
     * 采用无填充 Base64url 编码的凭据标识
     */
    private String credentialId;

    /**
     * 用户可见凭据名称
     */
    private String label;

    /**
     * 最近成功认证时间，尚未使用时为 null
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private OffsetDateTime lastUsedAt;
}
