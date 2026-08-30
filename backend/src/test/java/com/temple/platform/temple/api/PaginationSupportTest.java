package com.temple.platform.temple.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationSupportTest {

    @Test
    void resolveUsesDefaults() {
        PaginationSupport.PageRequest request = PaginationSupport.resolve(null, null);
        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(PaginationSupport.DEFAULT_SIZE);
        assertThat(request.offset()).isZero();
    }

    @Test
    void resolveAcceptsMaxSize() {
        PaginationSupport.PageRequest request = PaginationSupport.resolve(0, 100);
        assertThat(request.size()).isEqualTo(100);
        assertThat(request.offset()).isZero();
    }

    @Test
    void resolveRejectsOversizedPage() {
        assertThatThrownBy(() -> PaginationSupport.resolve(null, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum of 100");
    }

    @Test
    void resolveRejectsUnsafeOffset() {
        assertThatThrownBy(() -> PaginationSupport.resolve(30_000_000, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset exceeds maximum");
    }

    @Test
    void negativePageDefaultsToZero() {
        PaginationSupport.PageRequest request = PaginationSupport.resolve(-5, 20);
        assertThat(request.page()).isZero();
        assertThat(request.offset()).isZero();
    }
}
