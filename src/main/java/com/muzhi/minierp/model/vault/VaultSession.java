package com.muzhi.minierp.model.vault;

import com.muzhi.minierp.enums.VaultEnum;
import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * <p>
 * {@code VaultSession}：独立会话身份；不包含口令、令牌或解密材料
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public record VaultSession(Long ownerId, Long accountId, long authEpoch, VaultEnum.Stage stage,
        Instant loginAt, Instant passwordAt, Instant mfaAt, Instant recoveryUntil,
        UUID bindingId, boolean recovering) implements Principal, Serializable {

    @Override
    public String getName() {
        return ownerId.toString();
    }
}
