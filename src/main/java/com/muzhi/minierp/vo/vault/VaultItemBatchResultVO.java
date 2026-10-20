package com.muzhi.minierp.vo.vault;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * {@code VaultItemBatchResultVO}：账号组与独立条目原子变更的最小公开回执
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-18
 */
@Data
public class VaultItemBatchResultVO {

    /**
     * 本次变更所属活动代
     */
    private Long generation;

    /**
     * 整批提交后的集合版本
     */
    private Long collectionRevision;

    /**
     * 按请求顺序排列的逐项回执，不包含加密内容或明文关系
     */
    private List<VaultItemResultVO> results;
}
