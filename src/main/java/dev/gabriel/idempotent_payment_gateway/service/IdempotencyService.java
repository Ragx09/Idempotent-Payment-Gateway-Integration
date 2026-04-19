package dev.gabriel.idempotent_payment_gateway.service;

import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed idempotency receipts for REST payments vs webhook-driven payments (separate key namespaces).
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long TTL_HOURS = 24; // cache expires in 24h

    public TransactionResponseDto get(IdempotencyScope scope, String key) {
        return (TransactionResponseDto) redisTemplate.opsForValue().get(scope.getCachePrefix() + key);
    }

    public void save(IdempotencyScope scope, String key, TransactionResponseDto responseDto) {
        redisTemplate.opsForValue().set(
                scope.getCachePrefix() + key,
                responseDto,
                TTL_HOURS,
                TimeUnit.HOURS
        );
    }
}
