package com.temple.platform.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.function.Supplier;

public class RedisCatalogCache implements CatalogCache {

    private static final Logger log = LoggerFactory.getLogger(RedisCatalogCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCatalogCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        put(key, value, ttl);
        return value;
    }

    @Override
    public <T> T getOrLoad(String key, TypeReference<T> typeRef, Duration ttl, Supplier<T> loader) {
        T cached = get(key, typeRef);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        put(key, value, ttl);
        return value;
    }

    @Override
    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            redisTemplate.delete(java.util.Arrays.asList(keys));
        } catch (RuntimeException ex) {
            log.warn("Cache DEL failed for keys {}: {}", java.util.Arrays.toString(keys), ex.getMessage());
        }
    }

    private <T> T get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Cache GET failed for key {}: {}", key, ex.getMessage());
            return null;
        }
    }

    private <T> T get(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, typeRef);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Cache GET failed for key {}: {}", key, ex.getMessage());
            return null;
        }
    }

    private void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Cache SET failed for key {}: {}", key, ex.getMessage());
        }
    }
}
