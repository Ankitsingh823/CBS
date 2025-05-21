package com.controller;

import com.dto.ServiceConfigDTO;
import com.dto.ServiceConfigCompareDTO;
import com.model.ServiceConfigUpdates;
import com.model.ServiceConfiguration;
import com.repository.JPA.JPAServiceConfigUpdatesRepository;
import com.service.RedisService;
import com.service.ServiceConfigService;
import com.utils.GenricMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ServiceConfigController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ServiceConfigService serviceConfigService;

    @Autowired
    private JPAServiceConfigUpdatesRepository stagingRepository;

    @PostMapping("/create")
    public ResponseEntity<ServiceConfiguration> createServiceConfig(@RequestBody ServiceConfigDTO configDTO) {
        ServiceConfiguration config = serviceConfigService.createServiceConfig(
                configDTO.getName(),
                configDTO.getDescription(),
                configDTO.getValue(),
                configDTO.getCreatedBy()
        );
        return new ResponseEntity<>(config, HttpStatus.CREATED);
    }

    @GetMapping("/compare")
    public ResponseEntity<ServiceConfigCompareDTO> compareServiceConfigVersions(
            @RequestParam Long configId,
            @RequestParam(required = false) Integer version1,
            @RequestParam(required = false) Integer version2) {
        ServiceConfigCompareDTO comparison = serviceConfigService.compareServiceConfigVersions(configId, version1, version2);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ServiceConfiguration> getServiceConfigById(@PathVariable long id) {
        String cacheKey = "CONFIG:ID:" + id;

        String cachedJson = redisService.getValue(cacheKey);
        if (cachedJson != null) {
            try {
                ServiceConfiguration config = new ObjectMapper().readValue(cachedJson, ServiceConfiguration.class);
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                System.err.println("Failed to deserialize from Redis: " + e.getMessage());
            }
        }

        Optional<ServiceConfiguration> configOpt = serviceConfigService.getServiceConfigById(id);
        if (configOpt.isPresent()) {
            ServiceConfiguration config = configOpt.get();
            try {
                String json = new ObjectMapper().writeValueAsString(config);
                redisService.setValue(cacheKey, json, 3600);
            } catch (Exception e) {
                System.err.println("Failed to serialize for Redis: " + e.getMessage());
            }
            return ResponseEntity.ok(config);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Object> getServiceConfigByName(@PathVariable String name) {
        Object value = serviceConfigService.getServiceConfigValue(name);
        return ResponseEntity.ok(value != null ? value : "Config not approved or doesn't exist");
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ServiceConfiguration>> listServiceConfigs(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<ServiceConfiguration> configs = serviceConfigService.listServiceConfigs(name, status, pageable);
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceConfigUpdates> updateServiceConfig(
            @PathVariable Long id,
            @RequestBody ServiceConfigDTO configDTO) {

        serviceConfigService.updateServiceConfigValue(
                id,
                configDTO.getDescription(),
                configDTO.getValue(),
                configDTO.getUpdatedBy()
        );

        //Return the latest staging config
        Optional<ServiceConfigUpdates> stagingOpt = stagingRepository.findByConfigId(id);
        if (stagingOpt.isPresent()) {
            return ResponseEntity.ok(stagingOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceConfig(@PathVariable Long id) {
        Optional<ServiceConfiguration> configOpt = serviceConfigService.getServiceConfigById(id);
        configOpt.ifPresent(config -> redisService.deleteKey("CONFIG:" + config.getName()));
        serviceConfigService.deleteServiceConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invalidate-cache/{name}")
    public ResponseEntity<String> invalidateServiceConfigCache(@PathVariable String name) {
        redisService.deleteKey("CONFIG:" + name);
        serviceConfigService.invalidateServiceConfigCache(name);
        return ResponseEntity.ok("Cache invalidated for: " + name);
    }

    @GetMapping("/history/{configId}")
    public ResponseEntity<List<Map<String, Object>>> getConfigHistory(@PathVariable Long configId) {
        List<Map<String, Object>> history = serviceConfigService.getConfigHistory(configId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> createRelease(
            @RequestParam List<Long> configIds,
            @RequestParam String userEmail) {
        Map<String, Object> release = serviceConfigService.createRelease(configIds, userEmail);
        return new ResponseEntity<>(release, HttpStatus.CREATED);
    }

    @GetMapping("/rollout-enabled")
    public ResponseEntity<Boolean> isFeatureRolloutEnabled(
            @RequestParam String name,
            @RequestParam String entityId) {
        try {
            boolean enabled = serviceConfigService.getServiceConfigValue(name) instanceof Map
                    && new GenricMethods(serviceConfigService).isRolloutEnabled(name, entityId);

            return ResponseEntity.ok(enabled);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


//    @GetMapping("/rollout-enabled")
//    public ResponseEntity<Boolean> isFeatureRolloutEnabled(
//            @RequestParam String name,
//            @RequestParam String entityId) {
//        try {
//            GenricMethods genricMethods = new GenricMethods(serviceConfigService);  // Pass service
//            boolean enabled = genricMethods.isRolloutEnabled(name, entityId);       // Always call method
//            return ResponseEntity.ok(enabled);
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the issue
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
//        }
//    }


}



