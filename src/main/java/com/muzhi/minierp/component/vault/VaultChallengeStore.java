package com.muzhi.minierp.component.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.vo.vault.VaultOperationOptionsVO;
import com.muzhi.minierp.vo.vault.VaultWebAuthnOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
     * 每个操作使用独立随机标识，多个标签页不会覆盖彼此的待验证挑战。
     */
    public VaultOperationOptionsVO issueOperation(byte[] binding) {
        byte[] challenge = VaultCodec.random(32);
        byte[] identifier = VaultCodec.random(32);
        String challengeId = VaultCodec.encode(identifier);
        Instant expiresAt = Instant.now().plusSeconds(120);
        String key = "mini-erp:vault:operation-challenge:" + challengeId;
        String value = VaultCodec.encode(binding) + ":" + expiresAt.toEpochMilli() + ":" + VaultCodec.encode(challenge);
        redis.opsForValue().set(key, value, Duration.ofMinutes(2));
        VaultWebAuthnOptionsVO publicKey = new VaultWebAuthnOptionsVO();
        publicKey.setChallenge(VaultCodec.encode(challenge));
        publicKey.setTimeout(120000);
        VaultOperationOptionsVO result = new VaultOperationOptionsVO();
        result.setChallengeId(challengeId);
        result.setPublicKey(publicKey);
        result.setExpiresAt(expiresAt);
        return result;
    }

    /**
     * 先原子消费再校验绑定；业务回滚也不能恢复已经尝试过的挑战。
     */
    public byte[] consumeOperation(String challengeId, byte[] binding) {
        VaultCodec.decode(challengeId, 32, 32);
        String key = "mini-erp:vault:operation-challenge:" + challengeId;
        String value = redis.opsForValue().getAndDelete(key);
        String prefix = VaultCodec.encode(binding) + ":";
        VaultEnum.Error.OPERATION_AUTHORIZATION_INVALID.rejectIf(value == null || !value.startsWith(prefix));
        String[] parts = value.substring(prefix.length()).split(":", -1);
        VaultEnum.Error.OPERATION_AUTHORIZATION_INVALID.rejectIf(parts.length != 2
                || Instant.now().toEpochMilli() >= Long.parseLong(parts[0]));
        return VaultCodec.decode(parts[1], 32, 32);
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
