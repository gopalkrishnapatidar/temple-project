package com.temple.platform.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCatalogCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCatalogCache cache;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new RedisCatalogCache(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void cacheMissLoadsFromLoaderAndStoresValue() throws Exception {
        Temple temple = sampleTemple(1L);
        when(valueOperations.get(CacheKeys.templeId(1L))).thenReturn(null);

        AtomicInteger loads = new AtomicInteger();
        Temple loaded = cache.getOrLoad(
                CacheKeys.templeId(1L),
                Temple.class,
                Duration.ofMinutes(10),
                () -> {
                    loads.incrementAndGet();
                    return temple;
                });

        assertThat(loaded).isEqualTo(temple);
        assertThat(loads).hasValue(1);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(CacheKeys.templeId(1L)), jsonCaptor.capture(), eq(Duration.ofMinutes(10)));
        Temple roundTrip = objectMapper.readValue(jsonCaptor.getValue(), Temple.class);
        assertThat(roundTrip).isEqualTo(temple);
    }

    @Test
    void cacheHitAvoidsSecondLoaderCall() throws Exception {
        Temple temple = sampleTemple(2L);
        String json = objectMapper.writeValueAsString(temple);
        when(valueOperations.get(CacheKeys.templeId(2L))).thenReturn(json);

        AtomicInteger loads = new AtomicInteger();
        Temple first = cache.getOrLoad(
                CacheKeys.templeId(2L),
                Temple.class,
                Duration.ofMinutes(10),
                () -> {
                    loads.incrementAndGet();
                    return temple;
                });
        Temple second = cache.getOrLoad(
                CacheKeys.templeId(2L),
                Temple.class,
                Duration.ofMinutes(10),
                () -> {
                    loads.incrementAndGet();
                    return temple;
                });

        assertThat(first).isEqualTo(temple);
        assertThat(second).isEqualTo(temple);
        assertThat(loads).hasValue(0);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void getFailureFallsBackToLoader() {
        when(valueOperations.get(CacheKeys.templeId(3L))).thenThrow(new RuntimeException("redis down"));

        AtomicInteger loads = new AtomicInteger();
        Temple temple = sampleTemple(3L);
        Temple loaded = cache.getOrLoad(
                CacheKeys.templeId(3L),
                Temple.class,
                Duration.ofMinutes(10),
                () -> {
                    loads.incrementAndGet();
                    return temple;
                });

        assertThat(loaded).isEqualTo(temple);
        assertThat(loads).hasValue(1);
    }

    @Test
    void setFailureDoesNotFailBusinessOperation() {
        Temple temple = sampleTemple(4L);
        when(valueOperations.get(CacheKeys.templeId(4L))).thenReturn(null);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        Temple loaded = cache.getOrLoad(
                CacheKeys.templeId(4L),
                Temple.class,
                Duration.ofMinutes(10),
                () -> temple);

        assertThat(loaded).isEqualTo(temple);
    }

    @Test
    void deleteFailureDoesNotThrow() {
        when(redisTemplate.delete(java.util.List.of(CacheKeys.templeId(5L))))
                .thenThrow(new RuntimeException("redis down"));

        cache.delete(CacheKeys.templeId(5L));
    }

    @Test
    void isEnabledReturnsTrue() {
        assertThat(cache.isEnabled()).isTrue();
    }

    private static Temple sampleTemple(long id) {
        OffsetDateTime now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Temple(id, "Temple", "Desc", "City", "State", "IN", "Asia/Kolkata",
                TempleStatus.ACTIVE, now, now);
    }
}
