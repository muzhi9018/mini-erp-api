package com.muzhi.minierp.component.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * <p>
 * {@code VaultChallengeStore}：Redis 单次认证挑战和有界限流；不保存解密材料
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultChallengeStore {

    private final StringRedisTemplate redis;

    public byte[] issue(VaultSession session, VaultEnum.ChallengePurpose purpose, long revision) {
        byte[] challenge = VaultCodec.random(32);
        String value = session.accountId() + ":" + session.authEpoch() + ":" + revision + ":" + VaultCodec.encode(challenge);
        String key = this.key(session, purpose);
        redis.opsForValue().set(key, value, Duration.ofMinutes(2));
        return challenge;
    }

    public byte[] consume(VaultSession session, VaultEnum.ChallengePurpose purpose, long revision) {
        String key = this.key(session, purpose);
        String value = redis.opsForValue().getAndDelete(key);
        String prefix = session.accountId() + ":" + session.authEpoch() + ":" + revision + ":";
        VaultEnum.Error.CHALLENGE_INVALID.rejectIf(value == null || !value.startsWith(prefix));
        return VaultCodec.decode(value.substring(prefix.length()), 32, 32);
    }

    private String key(VaultSession session, VaultEnum.ChallengePurpose purpose) {
        return "mini-erp:vault:challenge:" + purpose.getCode() + ":" + session.bindingId();
    }

    /**
     * 账户与来源分别计数，首次计数同时设过期，失败后不永久锁定用户。
     */
    public void rate(String scope, String subject, int maximum) {
        byte[] hash = VaultCodec.sha256(subject.getBytes(StandardCharsets.UTF_8));
        String key = "mini-erp:vault:rate:" + scope + ":" + VaultCodec.encode(hash);
        String script = "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n";
        DefaultRedisScript<Long> operation = new DefaultRedisScript<>(script, Long.class);
        Long count = redis.execute(operation, List.of(key));
        VaultEnum.Error.UNAVAILABLE.rejectIf(count == null);
        VaultEnum.Error.RATE_LIMITED.rejectIf(count > maximum);
    }
}
