package com.muzhi.minierp.vo.vault;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.muzhi.minierp.model.vault.VaultProtocol;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * {@code VaultItemPageVO}：活动代与轮换暂存代共用的密文条目游标分页
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultItemPageVO {

    /**
     * 轮换任务标识，仅暂存代分页返回
     */
    private String rotationId;

    /**
     * 条目所属代
     */
    private Long generation;

    /**
     * 集合版本，仅活动代分页返回
     */
    private Long collectionRevision;

    /**
     * 当前页密文条目
     */
    private List<VaultProtocol.Item> items;

    /**
     * 下一页游标，最后一页明确返回 null
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private String nextCursor;
}
