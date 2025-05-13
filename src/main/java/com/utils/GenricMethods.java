package com.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.JPA.JPAServiceConfigRepository;
import com.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class GenricMethods {

    private static final String REDIS_PREFIX = "CONFIG:";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JPAServiceConfigRepository configRepository;

    private String getFromRedisOrDb(String configName) {
        String redisKey = REDIS_PREFIX + configName;
        String cachedValue = redisTemplate.opsForValue().get(redisKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        return configRepository.findByName(configName)
                .filter(config -> "APPROVED".equals(config.getStatus()))
                .map(config -> {
                    redisService.setValue(redisKey, config.getValue(), 600); // cache only APPROVED
                    return config.getValue();
                })
                .orElseThrow(() -> new RuntimeException("Approved configuration not found for: " + configName));
    }


    public Boolean getBooleanValue(String configName) {
        return Boolean.parseBoolean(getFromRedisOrDb(configName));
    }

    public Integer getIntegerValue(String configName) {
        return Integer.parseInt(getFromRedisOrDb(configName));
    }

    public List<String> getListValue(String configName) {
        String val = getFromRedisOrDb(configName);
        return Arrays.asList(val.replace("[", "").replace("]", "").replace("\"", "").split(","));
    }


    /**
     * Fetch and convert JSON config to specified POJO class
     */
    public <T> T getConfigAsObject(String configName, Class<T> clazz) {
        String json = getFromRedisOrDb(configName);
        return parseJsonConfig(json, clazz);
    }

    /**
     * Parse raw JSON into given class (internal)
     */
    public <T> T parseJsonConfig(String configValue, Class<T> clazz) {
        try {
            return objectMapper.readValue(configValue, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse config JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Fetch full config as raw JsonNode (for dynamic access)
     */
    public JsonNode getJsonNode(String configName) {
        try {
            return objectMapper.readTree(getFromRedisOrDb(configName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse config as JsonNode: " + configName, e);
        }
    }
}


