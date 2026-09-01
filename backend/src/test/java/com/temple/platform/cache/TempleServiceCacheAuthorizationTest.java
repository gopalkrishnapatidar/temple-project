package com.temple.platform.cache;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.security.AccountUserDetails;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleEventRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import com.temple.platform.temple.service.TempleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TempleServiceCacheAuthorizationTest {

    @Mock
    private TempleRepository templeRepository;

    @Mock
    private TempleAdminAssignmentRepository assignmentRepository;

    @Mock
    private TempleEventRepository eventRepository;

    @Mock
    private TempleAuthorizationService authorizationService;

    @Mock
    private CatalogCache catalogCache;

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private CacheInvalidationPublisher cacheInvalidation;

    @InjectMocks
    private TempleService templeService;

    @Test
    void cachedInactiveTempleStillHiddenFromDevotee() {
        Temple inactive = sampleTemple(10L, TempleStatus.INACTIVE);
        when(cacheProperties.getTempleIdTtl()).thenReturn(Duration.ofMinutes(10));
        when(catalogCache.getOrLoad(eq(CacheKeys.templeId(10L)), eq(Temple.class), any(), any()))
                .thenReturn(inactive);
        when(authorizationService.isPlatformAdmin(any())).thenReturn(false);
        when(authorizationService.isTempleAdmin(any())).thenReturn(false);
        when(authorizationService.requireAccountId(any())).thenReturn(99L);

        Authentication devotee = devoteeAuth(99L);

        assertThatThrownBy(() -> templeService.getTemple(10L, devotee))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Temple not found");
    }

    @Test
    void cachedActiveTempleVisibleToDevotee() {
        Temple active = sampleTemple(11L, TempleStatus.ACTIVE);
        when(cacheProperties.getTempleIdTtl()).thenReturn(Duration.ofMinutes(10));
        when(catalogCache.getOrLoad(eq(CacheKeys.templeId(11L)), eq(Temple.class), any(), any()))
                .thenReturn(active);
        when(authorizationService.isPlatformAdmin(any())).thenReturn(false);
        when(authorizationService.isTempleAdmin(any())).thenReturn(false);
        when(authorizationService.requireAccountId(any())).thenReturn(99L);

        Authentication devotee = devoteeAuth(99L);

        var response = templeService.getTemple(11L, devotee);
        org.assertj.core.api.Assertions.assertThat(response.id()).isEqualTo(11L);
    }

    @Test
    void cachedPublicTempleListStillFilteredByRoleForAdminPaths() {
        when(authorizationService.requireAccountId(any())).thenReturn(1L);
        when(authorizationService.isPlatformAdmin(any())).thenReturn(true);
        when(authorizationService.isTempleAdmin(any())).thenReturn(false);
        Temple temple = sampleTemple(1L, TempleStatus.ACTIVE);
        when(templeRepository.findAllVisible(true, 1L)).thenReturn(List.of(temple));

        var response = templeService.listTemples(devoteeAuth(1L));

        org.assertj.core.api.Assertions.assertThat(response).hasSize(1);
        org.mockito.Mockito.verify(catalogCache, org.mockito.Mockito.never())
                .getOrLoad(any(), any(com.fasterxml.jackson.core.type.TypeReference.class), any(), any());
    }

    private static Temple sampleTemple(long id, TempleStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Temple(id, "Temple", "Desc", "City", "State", "IN", "Asia/Kolkata", status, now, now);
    }

    private static Authentication devoteeAuth(long accountId) {
        AccountUserDetails principal = new AccountUserDetails(
                accountId, "devotee@example.com", "pw", AccountRole.DEVOTEE, AccountStatus.ACTIVE);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
