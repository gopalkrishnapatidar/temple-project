package com.temple.platform.temple.security;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.temple.exception.ForbiddenOperationException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TempleAuthorizationService {

    private final TempleAdminAssignmentRepository assignmentRepository;
    private final AccountRepository accountRepository;

    public TempleAuthorizationService(
            TempleAdminAssignmentRepository assignmentRepository,
            AccountRepository accountRepository) {
        this.assignmentRepository = assignmentRepository;
        this.accountRepository = accountRepository;
    }

    public long requireAccountId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenOperationException("Authentication required");
        }
        return Long.parseLong(authentication.getName());
    }

    public boolean isPlatformAdmin(Authentication authentication) {
        return hasRole(authentication, AccountRole.PLATFORM_ADMIN);
    }

    public boolean isTempleAdmin(Authentication authentication) {
        return hasRole(authentication, AccountRole.TEMPLE_ADMIN);
    }

    public void requirePlatformAdmin(Authentication authentication) {
        if (!isPlatformAdmin(authentication)) {
            throw new ForbiddenOperationException("Platform administrator access required");
        }
    }

    public void requireTempleManagement(Authentication authentication, long templeId) {
        if (isPlatformAdmin(authentication)) {
            return;
        }
        long accountId = requireAccountId(authentication);
        if (isTempleAdmin(authentication)
                && assignmentRepository.exists(accountId, templeId)) {
            return;
        }
        throw new ForbiddenOperationException("Temple management access denied");
    }

    public void requireAssignmentManagement(Authentication authentication) {
        requirePlatformAdmin(authentication);
    }

    public void requireAssignableTempleAdminAccount(long accountId) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ForbiddenOperationException("Account is not eligible for temple admin assignment"));
        if (account.status() != AccountStatus.ACTIVE || account.role() != AccountRole.TEMPLE_ADMIN) {
            throw new ForbiddenOperationException("Account is not eligible for temple admin assignment");
        }
    }

    private static boolean hasRole(Authentication authentication, AccountRole role) {
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role.name();
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
