package com.temple.platform.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CacheInvalidationPublisher {

    private final CatalogCache catalogCache;

    public CacheInvalidationPublisher(CatalogCache catalogCache) {
        this.catalogCache = catalogCache;
    }

    public void invalidateAfterCommit(String... keys) {
        if (!catalogCache.isEnabled() || keys == null || keys.length == 0) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            catalogCache.delete(keys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                catalogCache.delete(keys);
            }
        });
    }
}
