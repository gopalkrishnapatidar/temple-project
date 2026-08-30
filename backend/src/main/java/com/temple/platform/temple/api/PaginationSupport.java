package com.temple.platform.temple.api;

public final class PaginationSupport {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationSupport() {
    }

    private static final int MAX_OFFSET = Integer.MAX_VALUE;

    public record PageRequest(int page, int size) {

        public int offset() {
            return toSafeOffset(page, size);
        }
    }

    public static PageRequest resolve(Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? DEFAULT_SIZE : size;
        if (resolvedSize > MAX_SIZE) {
            throw new IllegalArgumentException("page size exceeds maximum of " + MAX_SIZE);
        }
        toSafeOffset(resolvedPage, resolvedSize);
        return new PageRequest(resolvedPage, resolvedSize);
    }

    private static int toSafeOffset(int page, int size) {
        long offset = (long) page * size;
        if (offset > MAX_OFFSET) {
            throw new IllegalArgumentException("page offset exceeds maximum");
        }
        return (int) offset;
    }
}
