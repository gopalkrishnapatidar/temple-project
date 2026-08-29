package com.temple.platform.identity.api;

import com.temple.platform.identity.api.dto.AccountResponse;
import com.temple.platform.identity.api.dto.LoginRequest;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.api.dto.RegisterRequest;
import com.temple.platform.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        long accountId = Long.parseLong(authentication.getName());
        return authService.currentUser(accountId);
    }
}
