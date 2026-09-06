package com.temple.platform.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring Boot integration test against the shared Testcontainers PostgreSQL instance.
 * Apply explicitly to tests that require a real PostgreSQL database; slice tests such as
 * {@code @WebMvcTest} should not use this annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(initializers = IsolatedPostgresInitializer.class)
public @interface IsolatedPostgresIntegrationTest {
}
