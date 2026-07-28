package com.chaewookim.accountbookformoms.global.config;

import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.DashboardResponse;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    // Board HOT 랭킹 캐시 (type + days + limit 키, cross-user hit)
    public static final String CACHE_BOARD_HOT = "board:hot";

    // 대시보드 (userId + yearMonth 키, 거래 수정 시 CacheEvict)
    public static final String CACHE_DASHBOARD = "dashboard";

    // Phase 2: 그룹 평균 캐시 (userId 미포함 키 → cross-user hit)
    public static final String CACHE_GROUP_BUDGET_AGE = "group:budget:age";
    public static final String CACHE_GROUP_BUDGET_AMOUNT = "group:budget:amount";
    public static final String CACHE_GROUP_BUDGET_CATEGORY = "group:budget:category";

    public static final String CACHE_GROUP_EXPENSE_AGE_FIXED = "group:expense:age:fixed";
    public static final String CACHE_GROUP_EXPENSE_AGE_VARIABLE = "group:expense:age:variable";
    public static final String CACHE_GROUP_EXPENSE_AMOUNT_FIXED = "group:expense:amount:fixed";
    public static final String CACHE_GROUP_EXPENSE_AMOUNT_VARIABLE = "group:expense:amount:variable";
    public static final String CACHE_GROUP_EXPENSE_CATEGORY_FIXED = "group:expense:category:fixed";
    public static final String CACHE_GROUP_EXPENSE_CATEGORY_VARIABLE = "group:expense:category:variable";

    public static final String CACHE_GROUP_INCOME_AGE_FIXED = "group:income:age:fixed";
    public static final String CACHE_GROUP_INCOME_AGE_VARIABLE = "group:income:age:variable";
    public static final String CACHE_GROUP_INCOME_AMOUNT_FIXED = "group:income:amount:fixed";
    public static final String CACHE_GROUP_INCOME_AMOUNT_VARIABLE = "group:income:amount:variable";
    public static final String CACHE_GROUP_INCOME_CATEGORY_FIXED = "group:income:category:fixed";
    public static final String CACHE_GROUP_INCOME_CATEGORY_VARIABLE = "group:income:category:variable";

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 그룹 평균 캐시: warm-up 스케줄(30분 주기)과 정합되도록 TTL 30분
        RedisCacheConfiguration groupConfig = defaultConfig.entryTtl(Duration.ofMinutes(30));

        // Board HOT 랭킹: 좋아요/조회수 변동 대비 5분 TTL
        RedisCacheConfiguration hotConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));

        // 대시보드: record DTO는 default typing과 호환되지 않아 타입 고정 직렬화 사용
        ObjectMapper dashboardMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonRedisSerializer<DashboardResponse> dashboardSerializer =
                new Jackson2JsonRedisSerializer<>(dashboardMapper, DashboardResponse.class);
        RedisCacheConfiguration dashboardConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(dashboardSerializer));

        List<String> groupCacheNames = List.of(
                CACHE_GROUP_BUDGET_AGE, CACHE_GROUP_BUDGET_AMOUNT, CACHE_GROUP_BUDGET_CATEGORY,
                CACHE_GROUP_EXPENSE_AGE_FIXED, CACHE_GROUP_EXPENSE_AGE_VARIABLE,
                CACHE_GROUP_EXPENSE_AMOUNT_FIXED, CACHE_GROUP_EXPENSE_AMOUNT_VARIABLE,
                CACHE_GROUP_EXPENSE_CATEGORY_FIXED, CACHE_GROUP_EXPENSE_CATEGORY_VARIABLE,
                CACHE_GROUP_INCOME_AGE_FIXED, CACHE_GROUP_INCOME_AGE_VARIABLE,
                CACHE_GROUP_INCOME_AMOUNT_FIXED, CACHE_GROUP_INCOME_AMOUNT_VARIABLE,
                CACHE_GROUP_INCOME_CATEGORY_FIXED, CACHE_GROUP_INCOME_CATEGORY_VARIABLE);

        Map<String, RedisCacheConfiguration> perCacheConfig = new java.util.HashMap<>();
        groupCacheNames.forEach(name -> perCacheConfig.put(name, groupConfig));
        perCacheConfig.put(CACHE_BOARD_HOT, hotConfig);
        perCacheConfig.put(CACHE_DASHBOARD, dashboardConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
