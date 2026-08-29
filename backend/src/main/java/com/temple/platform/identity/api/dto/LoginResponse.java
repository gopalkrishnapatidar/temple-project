package com.temple.platform.identity.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    @Override
    public String toString() {
        return "LoginResponse[accessToken=***, tokenType=" + tokenType + ", expiresInSeconds=" + expiresInSeconds + "]";
    }
}
