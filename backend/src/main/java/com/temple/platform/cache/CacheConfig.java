package com.temple.platform.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
    CatalogCache redisCatalogCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisCatalogCache(redisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.enabled", havingValue = "false")
    CatalogCache noOpCatalogCache() {
        return new NoOpCatalogCache();
    }
}
