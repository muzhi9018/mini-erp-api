package com.muzhi.minierp.vo.vault;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.OffsetDateTime;
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
public class VaultItemPageVO<T> {

    /**
     * 轮换任务标识，仅内部轮换分页返回
     */
    private String rotationId;

    /**
     * 条目所属代
     */
    private Long generation;

    /**
     * 集合版本，正常及已删除活动代分页返回
     */
    private Long collectionRevision;

    /**
     * 当前页密文条目
     */
    private List<T> items;

    /**
     * 取得业务锁后的数据库实时时间
     */
    private OffsetDateTime serverTime;

    /**
     * 轮换下载范围，参见 {@link com.muzhi.minierp.enums.VaultEnum.ItemScope}
     */
    private String scope;

    /**
     * 下一页游标，最后一页明确返回 null
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private String nextCursor;
}
