package com.service;

import com.dto.ServiceConfigCompareDTO;
import com.model.ServiceConfigVersion;
import com.model.ServiceConfiguration;
import com.model.ServiceConfigUpdates;
import com.repository.JPA.JPAServiceConfigRepository;
import com.repository.JPA.JPAServiceConfigVersionRepository;
import com.repository.JPA.JPAServiceConfigUpdatesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ServiceConfigService {

    @Autowired
    private JPAServiceConfigUpdatesRepository stagingRepository;

    @Autowired
    private JPAServiceConfigRepository configRepository;

    @Autowired
    private JPAServiceConfigVersionRepository versionRepository;

    @Autowired
    private RedisService redisService;
    //private RedisServiceConfigRepository redisConfigRepository;


    @Autowired
    private ObjectMapper objectMapper;

    private static final String REDIS_CONFIG_PREFIX = "CONFIG:";

    /**
     * Creates a new service configuration
     */
    @Transactional
    public ServiceConfiguration createServiceConfig(String name, String description, String value, String createdBy) {
        ServiceConfiguration config = new ServiceConfiguration();
        config.setId(generateUnique8DigitId()); // Set custom 8-digit ID
        config.setName(name);
        config.setDescription(description);
        config.setValue(value);
        config.setStatus("PENDING");
        config.setCreatedBy(createdBy);
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedAt(LocalDateTime.now());
        //config.setUpdatedBy();
        config.setVersion(0);
        ServiceConfiguration savedConfig = configRepository.save(config);

        // Create initial version
        ServiceConfigVersion version = new ServiceConfigVersion();
        version.setConfigId(savedConfig.getId());
        version.setValue(value);
        version.setVersion(0);
        version.setCreatedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        version.setStatus("PENDING");
        ServiceConfigVersion save = versionRepository.save(version);


        if ("APPROVED".equals(savedConfig.getStatus())) {
            String redisKey = REDIS_CONFIG_PREFIX + savedConfig.getName();
            redisService.setValue(redisKey, savedConfig.getValue(), 600);
//            System.out.println("REDIS_KEY" + redisKey);
//            System.out.println("REDIS_VALUE" + redisService.getValue(redisKey));
        }

        return savedConfig;

    }

    private Long generateUnique8DigitId() {
        Long id;
        do {
            id = ServiceConfiguration.generate8DigitId();
        } while (configRepository.existsById(id));
        return id;
    }

    /**
     * Gets a service configuration by ID
     */
    public Optional<ServiceConfiguration> getServiceConfigById(Long id) {
        return configRepository.findById(id);
    }

    /**
     * Gets a service configuration value by name
     */
    public Object getServiceConfigValue(String name) {
        // Try to get from Redis first
        String redisKey = REDIS_CONFIG_PREFIX + name;
        String cachedValue = redisService.getValue(redisKey);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, Object.class);
            } catch (JsonProcessingException e) {
                // Log error but continue to fetch from DB
            }
        }

        // If not in Redis or error parsing, get from DB
        Optional<ServiceConfiguration> config = configRepository.findByName(name);
        if (config.isPresent() && "APPROVED".equals(config.get().getStatus())) {
            String value = config.get().getValue();

            // Cache in Redis
            redisService.setValue(redisKey, value, 600); // 10 minutes TTL

            try {
                return objectMapper.readValue(value, Object.class);
            } catch (JsonProcessingException e) {
                return value;
            }
        }

        return null;
    }

    /**
     * Lists service configurations with optional filters
     */
    public Page<ServiceConfiguration> listServiceConfigs(String name, String status, Pageable pageable) {
        if (name != null && status != null) {
            return configRepository.findByNameContainingAndStatus(name, status, pageable);
        } else if (name != null) {
            return configRepository.findByNameContaining(name, pageable);
        } else if (status != null) {
            return configRepository.findByStatus(status, pageable);
        } else {
            return configRepository.findAll(pageable);
        }
    }

    /**
     * Updates a service configuration value
     */
    @Transactional
    public ServiceConfiguration updateServiceConfigValue(Long id, String description, String value, String updatedBy) {
        Optional<ServiceConfiguration> configOpt = configRepository.findById(id);
        if (!configOpt.isPresent()) {
            throw new RuntimeException("Configuration not found with id: " + id);
        }

        ServiceConfiguration existing = configOpt.get();

        int latestVersion = versionRepository.findMaxVersionForUpdate(id);
        int newVersion = (latestVersion == 0) ? 1 : latestVersion + 1;

        // Save a new staging row or update existing one
        ServiceConfigUpdates staging = stagingRepository.findByConfigId(id).orElse(new ServiceConfigUpdates());
        staging.setConfigId(id);
        staging.setName(existing.getName());
        staging.setDescription(description != null ? description : existing.getDescription());
        staging.setValue(value != null ? value : existing.getValue());
        staging.setStatus("PENDING");
        staging.setUpdatedAt(LocalDateTime.now());
        staging.setUpdatedBy(updatedBy);
        staging.setVersion(newVersion);

        stagingRepository.save(staging);

        // Invalidate Redis cache
        invalidateServiceConfigCache(existing.getName());

        return existing;
    }


    /**
     * Deletes a service configuration
     */
    @Transactional
    public void deleteServiceConfig(Long id) {
        Optional<ServiceConfiguration> configOpt = configRepository.findById(id);

        if (configOpt.isPresent()) {
            ServiceConfiguration config = configOpt.get();

            // Delete from Redis if exists
            String redisKey = REDIS_CONFIG_PREFIX + config.getName();
            redisService.deleteKey(redisKey);

            // Delete versions
            versionRepository.deleteByConfigId(id);

            // Delete config
            configRepository.deleteById(id);
        } else {
            throw new RuntimeException("Configuration not found with id: " + id);
        }
    }

    /**
     * Invalidates a service configuration cache
     */
    public void invalidateServiceConfigCache(String name) {
        String redisKey = REDIS_CONFIG_PREFIX + name;
        redisService.deleteKey(redisKey);
    }

    /**
     * Compares two versions of a service configuration
     */
    public ServiceConfigCompareDTO compareServiceConfigVersions(Long configId, Integer version1, Integer version2) {
        Optional<ServiceConfiguration> configOpt = configRepository.findById(configId);

        if (!configOpt.isPresent()) {
            throw new RuntimeException("Configuration not found with id: " + configId);
        }

        ServiceConfiguration config = configOpt.get();

        // If version2 is null, compare with current version
        String currentValue = config.getValue();
        Integer currentVersion = config.getVersion();

        // If version1 is null, use previous version
        if (version1 == null) {
            version1 = currentVersion - 1;
        }

        // If version2 is null, use current version
        if (version2 == null) {
            version2 = currentVersion;
        }

        // Get version1 value
        Optional<ServiceConfigVersion> v1Opt = versionRepository.findByConfigIdAndVersion(configId, version1);
        if (!v1Opt.isPresent()) {
            throw new RuntimeException("Version " + version1 + " not found for config id: " + configId);
        }

        String v1Value;
        if (version1.equals(currentVersion)) {
            v1Value = currentValue;
        } else {
            v1Value = v1Opt.get().getValue();
        }

        // Get version2 value
        String v2Value;
        if (version2.equals(currentVersion)) {
            v2Value = currentValue;
        } else {
            Optional<ServiceConfigVersion> v2Opt = versionRepository.findByConfigIdAndVersion(configId, version2);
            if (!v2Opt.isPresent()) {
                throw new RuntimeException("Version " + version2 + " not found for config id: " + configId);
            }
            v2Value = v2Opt.get().getValue();
        }

        // Create comparison DTO
        ServiceConfigCompareDTO compareDTO = new ServiceConfigCompareDTO();
        compareDTO.setConfigId(configId);
        compareDTO.setName(config.getName());
        compareDTO.setVersion1(version1);
        compareDTO.setVersion2(version2);
        compareDTO.setValue1(v1Value);
        compareDTO.setValue2(v2Value);

        return compareDTO;
    }

    /**
     * Gets the history of a configuration
     */
    public List<Map<String, Object>> getConfigHistory(Long configId) {
        List<ServiceConfigVersion> versions = versionRepository.findByConfigIdOrderByVersionDesc(configId);
        List<Map<String, Object>> history = new ArrayList<>();

        // Formatter for LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ServiceConfigVersion version : versions) {
            Map<String, Object> versionMap = new HashMap<>();
            versionMap.put("version", version.getVersion());
            versionMap.put("value", version.getValue());
            versionMap.put("status", version.getStatus());

            // Convert LocalDateTime to String with the formatter
            if (version.getCreatedAt() != null) {
                versionMap.put("createdAt", version.getCreatedAt().format(formatter));
            }

            versionMap.put("updatedBy", version.getUpdatedBy());
            history.add(versionMap);
        }

        return history;
    }


    /**
     * Creates a release for a set of configurations
     */
    @Transactional
    public Map<String, Object> createRelease(List<Long> configIds, String userEmail) {
        String releaseId = UUID.randomUUID().toString();

        for (Long configId : configIds) {
            Optional<ServiceConfiguration> configOpt = configRepository.findById(configId);
            Optional<ServiceConfigUpdates> stagingOpt = stagingRepository.findByConfigId(configId);

            if (configOpt.isPresent() && stagingOpt.isPresent()) {
                ServiceConfiguration main = configOpt.get();
                ServiceConfigUpdates staged = stagingOpt.get();

                // Update main config
                main.setDescription(staged.getDescription());
                main.setValue(staged.getValue());
                main.setStatus("APPROVED");
                main.setUpdatedBy(userEmail);
                main.setUpdatedAt(LocalDateTime.now());
                main.setVersion(staged.getVersion());

                configRepository.save(main);

                // Save version
                ServiceConfigVersion version = new ServiceConfigVersion();
                version.setConfigId(main.getId());
                version.setValue(main.getValue());
                version.setVersion(main.getVersion());
                version.setCreatedAt(LocalDateTime.now());
                version.setStatus("APPROVED");
                version.setUpdatedBy(userEmail);
                versionRepository.save(version);

                // Clean up staging
                stagingRepository.deleteByConfigId(configId);

                // Invalidate and refresh Redis
                String redisKey = REDIS_CONFIG_PREFIX + main.getName();
                redisService.deleteKey(redisKey);
                redisService.setValue(redisKey, main.getValue(), 600);
            }
        }

        Map<String, Object> release = new HashMap<>();
        release.put("id", releaseId);
        release.put("userEmail", userEmail);
        release.put("releaseId", "r-" + releaseId.substring(0, 8));
        release.put("releaseStatus", "COMPLETED");
        release.put("dateCreated", LocalDateTime.now());
        release.put("configIds", configIds);

        return release;
    }

    /**
     * Scheduled job to refresh Redis cache every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void refreshRedisCache() {
        List<ServiceConfiguration> configs = configRepository.findByStatus("APPROVED");

        for (ServiceConfiguration config : configs) {
            String redisKey = REDIS_CONFIG_PREFIX + config.getName();
            redisService.setValue(redisKey, config.getValue(), 600); // 10 minutes TTL
        }
    }

}

