package com.temple.platform.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.function.Supplier;

public class NoOpCatalogCache implements CatalogCache {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        return loader.get();
    }

    @Override
    public <T> T getOrLoad(String key, TypeReference<T> typeRef, Duration ttl, Supplier<T> loader) {
        return loader.get();
    }

    @Override
    public void delete(String... keys) {
        // no-op
    }
}
