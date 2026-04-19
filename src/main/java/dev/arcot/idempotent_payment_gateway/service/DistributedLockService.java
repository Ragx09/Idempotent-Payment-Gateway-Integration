package dev.gabriel.idempotent_payment_gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis-based distributed locks (SET NX + token + Lua unlock) for coordinating idempotency keys
 * and webhook ingress across multiple payment gateway instances—part of the consistency story
 * validated under high parallel load (4K+ request k6 profile).
 */
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();
    static {
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public boolean tryLock(String lockKey, String token, Duration ttl) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String lockKey, String token) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
    }

    public String newToken() {
        return UUID.randomUUID().toString();
    }
}
