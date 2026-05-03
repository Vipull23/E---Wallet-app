package org.gfg.UserService.config;

import io.lettuce.core.resource.DefaultClientResources;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.gfg.UserService.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class Config {


    @Bean
    public RedisTemplate<String, User> redisTemplate(RedisConnectionFactory connectionFactory){
       RedisTemplate<String,User> redisTemplate = new RedisTemplate<>();
       redisTemplate.setConnectionFactory(connectionFactory);
       redisTemplate.setKeySerializer(new StringRedisSerializer());
       redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
       return redisTemplate;
    }

    @Bean(name = "otpRedis")
    public RedisTemplate<String, String> otpRedisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<String,String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Set up the RedisConnectionFactory programmatically
        String redisHost = "redis-15111.c264.ap-south-1-1.ec2.redns.redis-cloud.com";
        int redisPort = 15111; // Default port, change if necessary
        String redisPassword = "vgpt2ghQF5GwnvFdEuOAOeAOry5JcLVB";

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        factory.setPassword(redisPassword); // Optional if Redis requires a password
        factory.setClientResources(DefaultClientResources.create()); // Optional for custom resources

        return factory;
    }


    @Bean
    public KafkaTemplate<String,String> kafkaTemplate(){
        KafkaTemplate<String,String> kafkaTemplate = new KafkaTemplate<>(producerFactory());
        return kafkaTemplate;
    }

    @Bean
    public ProducerFactory<String,String> producerFactory(){
        Map<String,Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }



}
