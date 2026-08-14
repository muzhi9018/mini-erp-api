package com.muzhi.minierp.config;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * <p>
 * {@code Test}: redisTemplate 配置
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021/6/29 18:29
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setDefaultSerializer(fastJsonRedisSerializer());
        return redisTemplate;
    }

    /**
     * FastJSON序列序列化
     * @author Mr.Muzhi
     * @since 2022/11/29 11:54
     * @return FastJsonRedisSerializer
     */
    public RedisSerializer<?> fastJsonRedisSerializer() {
        return new GenericFastJsonRedisSerializer();
    }

}
