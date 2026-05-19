package com.hireflow.hireflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@Profile("!test")
@EnableCaching
public class RedisCacheConfig {

    public static final String JOB_LISTINGS = "jobListings";
    public static final String JOB_LISTINGS_OPEN = "jobListingsOpen";
    public static final String COMPANIES = "companies";
    public static final String SKILLS_ALL = "skillsAll";
    public static final String SKILL_SEARCH = "skillSearch";
    public static final String ADMIN_METRICS_VOLUME = "adminMetricsVolume";
    public static final String ADMIN_METRICS_TTH = "adminMetricsTth";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = baseConfig(objectMapper).entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(JOB_LISTINGS,           defaults.entryTtl(Duration.ofMinutes(30)));
        perCache.put(JOB_LISTINGS_OPEN,      defaults.entryTtl(Duration.ofMinutes(5)));
        perCache.put(COMPANIES,              defaults.entryTtl(Duration.ofHours(1)));
        perCache.put(SKILLS_ALL,             defaults.entryTtl(Duration.ofHours(1)));
        perCache.put(SKILL_SEARCH,           defaults.entryTtl(Duration.ofMinutes(30)));
        perCache.put(ADMIN_METRICS_VOLUME,   defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put(ADMIN_METRICS_TTH,      defaults.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    private RedisCacheConfiguration baseConfig(ObjectMapper objectMapper) {
        // Reuse the application ObjectMapper so JSR-310 (Instant) and other
        // already-registered modules apply to cached payloads.
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(name -> "hireflow::" + name + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }
}
