package com.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.EntityRollout;
import com.model.RolloutConfig;
import com.repository.JPA.JPAServiceConfigRepository;
import com.service.RedisService;
import com.service.ServiceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

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

    @Autowired
    private ServiceConfigService serviceConfigService;

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


    public GenricMethods(ServiceConfigService serviceConfigService) {
        this.serviceConfigService = serviceConfigService;
    }

    //Rollout based Generic Methods
    public boolean isRolloutEnabled(String featureName, String entityId) {
        Object config = serviceConfigService.getServiceConfigValue(featureName);

        System.out.println("CONFIG_NAME_ROLLOUT type: " + (config == null ? "null" : config.getClass().getName()));
        System.out.println("CONFIG_NAME_ROLLOUT value: " + config);

        if (config == null) {
            System.err.println("No config found for feature: " + featureName);
            return false;
        }

        try {
            RolloutConfig rolloutConfig;

            if (config instanceof String jsonString) {
                rolloutConfig = objectMapper.readValue(jsonString, RolloutConfig.class);
            } else {
                // For any other object (Map, LinkedHashMap, etc), serialize to JSON and deserialize properly
                String json = objectMapper.writeValueAsString(config);
                rolloutConfig = objectMapper.readValue(json, RolloutConfig.class);
            }

            return evaluateRollout(featureName, entityId, rolloutConfig);

        } catch (Exception e) {
            System.err.println("Invalid rollout config format for: " + featureName + " : " + e.getMessage());
            return false;
        }
    }



    private boolean evaluateRollout(String featureName, String entityId, RolloutConfig config) {
        String id = entityId.toLowerCase();

        // 1. Check disable list
        if (config.getDisableAny() != null &&
                config.getDisableAny().stream().map(String::toLowerCase).anyMatch(id::equals)) {
            System.out.println("Entity is in disableAny list");  //logs
            return false;
        }

        // 2. Global rollout
        if (Boolean.TRUE.equals(config.getEnableAll())) {
            Integer rollout = config.getEnableAllRollout();
            int bucket = getBucket(featureName + ":" + id);
            System.out.println("Global rollout: " + rollout + " | Bucket: " + bucket); //logs
            return rollout == null || bucket < rollout;
        }

        // 3. Per-entity rollout
        if (config.getEntities() != null) {
            Optional<EntityRollout> match = config.getEntities()
                    .stream()
                    .filter(e -> id.equalsIgnoreCase(e.getId()))
                    .findFirst();
            if (match.isPresent()) {
                int rollout = match.get().getRollout();
                int bucket = getBucket(featureName + ":" + id);
                System.out.println("Entity: " + id + " | Rollout: " + rollout + " | Bucket: " + bucket); //logs
                return bucket < rollout;
            } else {
                System.out.println("Entity not found in config entities");
            }
        }

        return false;
    }


    private int getBucket(String key) {
        int bucket = ThreadLocalRandom.current().nextInt(100);
        System.out.println("Generated random bucket for key [" + key + "]: " + bucket);
        return bucket;
    }



