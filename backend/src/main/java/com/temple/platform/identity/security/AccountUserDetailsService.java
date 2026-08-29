package com.temple.platform.identity.security;

import com.temple.platform.identity.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = EmailNormalizer.normalize(username);
        return accountRepository.findByEmail(email)
                .map(account -> new AccountUserDetails(
                        account.id(),
                        account.email(),
                        account.passwordHash(),
                        account.role(),
                        account.status()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
