package com.gymcrm.config;

import com.gymcrm.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ComponentScan(basePackages = "com.gymcrm")
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(name = "traineeMap")
    public Map<Long, Object> traineeMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "trainerMap")
    public Map<Long, Object> trainerMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "trainingMap")
    public Map<Long, Object> trainingMap() {
        return new ConcurrentHashMap<>();
    }
}
