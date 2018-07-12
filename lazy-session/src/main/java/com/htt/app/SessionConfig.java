package com.htt.app;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import redis.clients.jedis.JedisPoolConfig;


@EnableRedisHttpSession
public class SessionConfig {
//    @Bean
//    public LettuceConnectionFactory connectionFactory() {
//        LettuceConnectionFactory factory = new LettuceConnectionFactory();
//        factory.setHostName("192.168.153.128");
//        factory.setPort(6379);
//        factory.setDatabase(0);
//        factory.setTimeout(3000);
//        return factory;
//    }

    private static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    static {
        jedisPoolConfig.setMaxTotal(100);
        jedisPoolConfig.setMaxIdle(100);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setMinIdle(8);
        jedisPoolConfig.setMaxWaitMillis(10000L);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000L);
        jedisPoolConfig.setNumTestsPerEvictionRun(10);
        jedisPoolConfig.setMinEvictableIdleTimeMillis(60000L);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        JedisConnectionFactory jsf = new JedisConnectionFactory();
        jsf.setPoolConfig(jedisPoolConfig);
        jsf.setHostName("192.168.153.128");
        jsf.setPort(6379);
        jsf.setTimeout(3000);
        jsf.setDatabase(0);
        return jsf;
    }

    /**
     * spring-session默认对key的序列化器是 StringRedisSerializer
     * 对value的序列化器是 JdkSerializationRedisSerializer
     * 此处针对 value 构造一个springSessionDefaultRedisSerializer的Bean使用fastjson的序列化器来覆盖默认的
     * @link https://docs.spring.io/spring-session/docs/current/reference/html5/#custom-redisserializer
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(){
        return new GenericFastJsonRedisSerializer();
    }

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<String,Object>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(springSessionDefaultRedisSerializer());
        redisTemplate.setHashValueSerializer(springSessionDefaultRedisSerializer());
        return redisTemplate;
    }


    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory){
        return new StringRedisTemplate(factory);
    }
}
