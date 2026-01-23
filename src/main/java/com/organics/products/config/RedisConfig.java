package com.organics.products.config;

import com.organics.products.EventListner.EventListener;
import com.organics.products.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();

        if (sentinel != null &&
                sentinel.getMaster() != null &&
                sentinel.getNodes() != null &&
                !sentinel.getNodes().isEmpty()) {

            RedisSentinelConfiguration sentinelConfig =
                    new RedisSentinelConfiguration()
                            .master(sentinel.getMaster());

            for (String s : sentinel.getNodes()) {
                String[] parts = s.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid sentinel node: " + s);
                }
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            }

            if (redisProperties.getPassword() != null &&
                    !redisProperties.getPassword().isBlank()) {

                sentinelConfig.setPassword(
                        RedisPassword.of(redisProperties.getPassword()));
            }

            System.out.println("Starting Redis in SENTINEL mode");
            return new LettuceConnectionFactory(sentinelConfig);
        }

        RedisStandaloneConfiguration redisConfig =
                new RedisStandaloneConfiguration();

        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());

        if (redisProperties.getPassword() != null &&
                !redisProperties.getPassword().isBlank()) {

            redisConfig.setPassword(
                    RedisPassword.of(redisProperties.getPassword()));
        }

        System.out.println("Starting Redis in STANDALONE mode");
        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                )
                .build();
    }

    @Bean
    public RedisTemplate<String, Notification> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Notification> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplateObject(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public MessageListenerAdapter messageListener(EventListener eventListener) {
        // Spring automatically injects the @Component EventListener here
        return new MessageListenerAdapter(eventListener);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("redis-listener-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListener) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(taskExecutor());
        container.addMessageListener(
                messageListener,
                new ChannelTopic("notifications:all")
        );

        return container;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}