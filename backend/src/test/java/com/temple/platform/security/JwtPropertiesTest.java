package com.temple.platform.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void blankSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties(" ", "temple-platform", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "temple-platform", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }
}
