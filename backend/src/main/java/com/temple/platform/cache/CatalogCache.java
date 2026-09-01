package com.temple.platform.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.function.Supplier;

public interface CatalogCache {

    boolean isEnabled();

    <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    <T> T getOrLoad(String key, TypeReference<T> typeRef, Duration ttl, Supplier<T> loader);

    void delete(String... keys);
}
