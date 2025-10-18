package splitwise.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration
 * 
 * Currently using simple in-memory caching with ConcurrentMapCacheManager.
 * For production with distributed systems, consider enabling Redis:
 * 1. Uncomment Redis dependency in pom.xml
 * 2. Configure Redis connection in application.yaml
 * 3. Replace ConcurrentMapCacheManager with RedisCacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Simple in-memory cache (TEMP CHANGES)
        return new ConcurrentMapCacheManager("balances", "users", "groups", "expenses");
    }
    
    /* Redis-based caching
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
    */
}