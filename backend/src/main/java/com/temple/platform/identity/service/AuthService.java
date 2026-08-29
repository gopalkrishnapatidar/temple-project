package com.temple.platform.identity.service;

import com.temple.platform.identity.api.dto.AccountResponse;
import com.temple.platform.identity.api.dto.LoginRequest;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.api.dto.RegisterRequest;
import com.temple.platform.identity.domain.Account;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.exception.DuplicateEmailException;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.identity.security.AccountUserDetails;
import com.temple.platform.identity.security.EmailNormalizer;
import com.temple.platform.security.JwtProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Lazy AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (accountRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        try {
            Account account = accountRepository.insert(
                    email,
                    passwordHash,
                    AccountRole.DEVOTEE,
                    AccountStatus.ACTIVE
            );
            return toResponse(account);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException();
        }
    }

    public LoginResponse login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );
        AccountUserDetails principal = (AccountUserDetails) authentication.getPrincipal();
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(jwtProperties.issuer())
                        .subject(Long.toString(principal.getAccountId()))
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .claim("role", principal.getRole().name())
                        .build()
        )).getTokenValue();
        return new LoginResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    @Transactional(readOnly = true)
    public AccountResponse currentUser(long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new org.springframework.security.authentication.InsufficientAuthenticationException(
                        "Account not found"
                ));
        return toResponse(account);
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.id(), account.email(), account.role(), account.status());
    }
}
