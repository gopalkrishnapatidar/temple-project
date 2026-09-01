package com.temple.platform.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpCatalogCacheTest {

    private final NoOpCatalogCache cache = new NoOpCatalogCache();

    @Test
    void disabledCacheAlwaysLoadsFromLoader() {
        AtomicInteger loads = new AtomicInteger();
        String value = cache.getOrLoad(
                CacheKeys.templeId(1L),
                String.class,
                Duration.ofMinutes(1),
                () -> {
                    loads.incrementAndGet();
                    return "loaded";
                });

        assertThat(value).isEqualTo("loaded");
        assertThat(loads).hasValue(1);
        assertThat(cache.isEnabled()).isFalse();
    }
}
