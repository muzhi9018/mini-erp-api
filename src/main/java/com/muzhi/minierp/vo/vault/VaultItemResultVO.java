package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultItemResultVO}：条目写入和删除的公开回执
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
}
