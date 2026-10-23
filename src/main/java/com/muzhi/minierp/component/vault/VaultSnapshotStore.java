package com.muzhi.minierp.component.vault;

import com.alibaba.fastjson2.JSONWriter;
import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.entity.vault.VaultItem;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.vault.VaultProtocol;
import com.muzhi.minierp.model.vault.VaultSnapshot;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.service.vault.IVaultItemService;
import com.muzhi.minierp.vo.vault.VaultMetaVO;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * {@code VaultSnapshotStore}：在业务锁保护下以游标生成快照，网络传输不占用数据库锁
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultSnapshotStore {

    private final IVaultItemService vaultItemService;

    private final VaultCipherStore vaultCipherStore;

    /**
     * 必须在持有账户与库行锁的事务内调用；只逐条转换密文，不构造全量集合。
     */
    public VaultSnapshot prepare(VaultInfo vault, VaultSession session) throws IOException {
        VaultMetaVO meta = vaultCipherStore.meta(vault);
        Long keyEpoch = meta.getKeyEpoch();
        VaultSnapshot snapshot = VaultSnapshot.create();
        boolean completed = false;
        try {
            try (Cursor<VaultItem> cursor = vaultItemService.snapshotItems(vault.getId(), vault.getActiveGeneration());
                    OutputStream output = snapshot.output();
                    JSONWriter json = JSONWriter.ofUTF8(JSONWriter.Feature.WriteNulls)) {
                JsonResult<?> envelope = JsonResult.success();
                json.startObject();
                json.writeNameValue("code", envelope.getCode());
                json.writeNameValue("message", envelope.getMessage());
                json.writeNameValue("success", envelope.isSuccess());
                json.writeName("data");
                json.writeColon();
                json.startObject();
                json.writeNameValue("initialized", meta.isInitialized());
                json.writeNameValue("vaultId", meta.getVaultId());
                json.writeNameValue("status", meta.getStatus());
                json.writeNameValue("generation", meta.getGeneration());
                json.writeNameValue("metaRevision", meta.getMetaRevision());
                json.writeNameValue("collectionRevision", meta.getCollectionRevision());
                json.writeNameValue("keyEpoch", keyEpoch);
                json.writeNameValue("keySlots", meta.getKeySlots());
                json.writeNameValue("ownerSubject", session.ownerId().toString());
                json.writeName("items");
                json.writeColon();
                json.startArray();
                json.flushTo(output);
                int count = 0;
                int maximumItems = 2000;
                for (VaultItem item : cursor) {
                    count++;
                    VaultEnum.Error.VERSION_CONFLICT.rejectIf(count > maximumItems || !keyEpoch.equals(item.getKeyEpoch()));
                    if (count > 1) {
                        json.writeComma();
                    }
                    VaultProtocol.Item encrypted = vaultCipherStore.view(item);
                    json.writeAny(encrypted);
                    // 每条密文落盘后清空缓冲区，避免 Fastjson2 在内存中积累整个快照。
                    json.flushTo(output);
                }
                json.endArray();
                json.endObject();
                json.endObject();
                json.flushTo(output);
            }
            completed = true;
            return snapshot;
        } finally {
            if (!completed) {
                snapshot.close();
            }
        }
    }
}
