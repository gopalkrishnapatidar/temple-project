package com.temple.platform.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationPublisherTest {

    @Mock
    private CatalogCache catalogCache;

    @Test
    void invalidateAfterCommitDeletesWhenNoTransactionActive() {
        whenEnabled();
        CacheInvalidationPublisher publisher = new CacheInvalidationPublisher(catalogCache);

        publisher.invalidateAfterCommit(CacheKeys.templeId(1L), CacheKeys.publicTempleList());

        verify(catalogCache).delete(CacheKeys.templeId(1L), CacheKeys.publicTempleList());
    }

    @Test
    void invalidateAfterCommitDeletesOnlyAfterSuccessfulCommit() {
        whenEnabled();
        CacheInvalidationPublisher publisher = new CacheInvalidationPublisher(catalogCache);
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.invalidateAfterCommit(CacheKeys.templeId(2L));

            verify(catalogCache, never()).delete(CacheKeys.templeId(2L));

            java.util.List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.getFirst().afterCommit();

            verify(catalogCache).delete(CacheKeys.templeId(2L));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void invalidateAfterCommitSkipsWhenCacheDisabled() {
        when(catalogCache.isEnabled()).thenReturn(false);
        CacheInvalidationPublisher publisher = new CacheInvalidationPublisher(catalogCache);

        publisher.invalidateAfterCommit(CacheKeys.templeId(3L));

        verify(catalogCache, never()).delete(CacheKeys.templeId(3L));
    }

    @Test
    void rollbackDoesNotRunAfterCommitSynchronization() {
        AtomicBoolean afterCommitRan = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    afterCommitRan.set(true);
                }
            });
            TransactionSynchronizationManager.getSynchronizations().getFirst().afterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK);
            assertThat(afterCommitRan).isFalse();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void whenEnabled() {
        when(catalogCache.isEnabled()).thenReturn(true);
    }
}
