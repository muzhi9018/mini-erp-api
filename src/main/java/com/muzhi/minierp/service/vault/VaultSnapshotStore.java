package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.entity.vault.VaultItem;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.VaultItemMapper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.vault.VaultSnapshot;
import com.muzhi.minierp.model.vault.VaultSession;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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

    private final VaultItemMapper items;

    private final VaultCipherStore cipher;

    private final ObjectMapper objectMapper;

    /**
     * 必须在持有账户与库行锁的事务内调用；只逐条转换密文，不构造全量集合。
     */
    public VaultSnapshot prepare(VaultInfo vault, VaultSession session) throws IOException {
        Map<String, Object> meta = cipher.meta(vault);
        meta.put("ownerSubject", session.ownerId().toString());
        Long keyEpoch = (Long) meta.get("keyEpoch");
        VaultSnapshot snapshot = VaultSnapshot.create();
        boolean completed = false;
        try {
            try (Cursor<VaultItem> cursor = items.snapshotItems(vault.getId(), vault.getActiveGeneration());
                    OutputStream output = snapshot.output();
                    JsonGenerator json = objectMapper.createGenerator(output)) {
                JsonResult<?> envelope = JsonResult.success();
                json.writeStartObject();
                json.writeNumberProperty("code", envelope.getCode());
                json.writeStringProperty("message", envelope.getMessage());
                json.writeBooleanProperty("success", envelope.isSuccess());
                json.writeName("data").writeStartObject();
                for (Map.Entry<String, Object> entry : meta.entrySet()) {
                    json.writeName(entry.getKey()).writePOJO(entry.getValue());
                }
                json.writeName("items").writeStartArray();
                int count = 0;
                int maximumItems = 2000;
                for (VaultItem item : cursor) {
                    count++;
                    VaultEnum.Error.VERSION_CONFLICT.rejectIf(count > maximumItems || !keyEpoch.equals(item.getKeyEpoch()));
                    var encrypted = cipher.view(item);
                    json.writePOJO(encrypted);
                }
                json.writeEndArray().writeEndObject().writeEndObject();
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
