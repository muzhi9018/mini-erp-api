package com.muzhi.minierp.vo.vault;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * <p>
 * {@code VaultItemResultVO}：条目写入和删除的公开回执
 * 时间输出沿用带偏移的 ISO 8601 字符串，保留完整精度并兼容已持久化回执。
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultItemResultVO {

    /**
     * 创建或更新的条目标识
     */
    private String itemId;

    /**
     * 写入后的行版本
     */
    private Long rowVersion;

    /**
     * 写入后的密文内容版本
     */
    private Long contentVersion;

    /**
     * 条目是否已删除，仅删除回执返回
     */
    private Boolean deleted;

    /**
     * 操作后的集合版本
     */
    private Long collectionRevision;

    /**
     * 删除时间
     */
    private OffsetDateTime deletedAt;

    /**
     * 永久删除期限，严格早于此时间才允许恢复
     */
    private OffsetDateTime purgeAt;

    /**
     * 是否已永久删除，仅永久删除回执返回
     */
    private Boolean permanentlyDeleted;

    /**
     * 实际永久删除时间
     */
    private OffsetDateTime purgedAt;

    /**
     * 取得业务锁后的数据库实时时间
     */
    private OffsetDateTime serverTime;
}
