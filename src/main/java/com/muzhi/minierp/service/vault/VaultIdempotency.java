package com.muzhi.minierp.service.vault;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.muzhi.minierp.entity.vault.VaultIdempotencyRecord;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.model.vault.VaultSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

/**
 * <p>
 * {@code VaultIdempotency}：事务内持久化最小公开回执，不缓存认证或解密秘密
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultIdempotency {
    private final VaultControlMapper control;

    private final VaultIdempotencyRecordMapper receipts;

    private final VaultAccess access;

    /**
     * 在当前事务中执行操作，重试时按同一响应类型读取已持久化的公开回执。
     */
    public <T> T execute(
            VaultSession session,
            VaultEnum.Operation operation,
            String key,
            Object payload,
            Class<T> responseType,
            Supplier<T> action
    ) {
        UUID id = VaultCodec.uuid(key);
        byte[] digest = VaultCodec.digest(List.of(session.ownerId().toString(), operation.getCode(), payload));
        VaultIdempotencyRecord previous = control.receipt(session.accountId(), operation.getCode(), id);
        if (previous != null) {
            VaultEnum.Error.IDEMPOTENCY_CONFLICT.rejectIf(!MessageDigest.isEqual(previous.getRequestDigest(), digest));
            return VaultCodec.receipt(previous.getResponseBody(), responseType);
        }
        T result = action.get();
        VaultIdempotencyRecord record = new VaultIdempotencyRecord();
        record.setId(IdWorker.getId());
        record.setAccountId(session.accountId());
        record.setOperation(operation.getCode());
        record.setIdempotencyKey(id);
        record.setRequestDigest(digest);
        record.setHttpStatus(200);
        record.setResponseBody(VaultCodec.json(result));
        access.create(record, session.ownerId());
        receipts.insert(record);
        return result;
    }
}
