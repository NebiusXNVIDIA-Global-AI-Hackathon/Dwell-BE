package com.dwell.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Kept separate from the main class because it would break slice tests such as @WebMvcTest
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
