package com.controller;

import com.dto.ServiceConfigDTO;
import com.dto.ServiceConfigCompareDTO;
import com.model.ServiceConfiguration;
import com.service.RedisService;
import com.service.ServiceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @PostMapping("/create")
    public ResponseEntity<ServiceConfiguration> createServiceConfig(@RequestBody ServiceConfigDTO configDTO) {
        ServiceConfiguration config = serviceConfigService.createServiceConfig(
                configDTO.getName(),
                configDTO.getDescription(),
                configDTO.getValue()
        );

        // Cache the new config value
        redisService.setValue("CONFIG:" + config.getName(), config.getValue(), 3600); // cache for 1 hour

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
    public ResponseEntity<ServiceConfiguration> getServiceConfigById(@PathVariable long id)  {
        String cacheKey = "CONFIG:ID:" + id;

        //Check Redis
        String cachedJson = redisService.getValue(cacheKey);
        if (cachedJson != null) {
            try{
                ServiceConfiguration config = new ObjectMapper().readValue(cachedJson, ServiceConfiguration.class);
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                System.err.println("Failed to deserialize from Redis: " + e.getMessage());
            }
        }

        //Fallback to DB
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
        String cacheKey = "CONFIG:" + name;

        // Fetch from Redis
        String cachedValue = redisService.getValue(cacheKey);
        if (cachedValue != null) {
            System.out.println("[Redis] Cache hit for key: " + cacheKey);
            return ResponseEntity.ok("From Cache: " + cachedValue);
        }

        // Fetch from DB
        System.out.println("[DB] Cache miss, fetching from DB for: " + name);
        Object value = serviceConfigService.getServiceConfigValue(name);
        if (value != null) {
            redisService.setValue(cacheKey, value.toString(), 3600);
        }

        return ResponseEntity.ok("From DB: " + value);
    }


    @GetMapping
    public ResponseEntity<Page<ServiceConfiguration>> listServiceConfigs(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<ServiceConfiguration> configs = serviceConfigService.listServiceConfigs(name, status, pageable);
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceConfiguration> updateServiceConfig(
            @PathVariable Long id,
            @RequestBody ServiceConfigDTO configDTO) {
        ServiceConfiguration config = serviceConfigService.updateServiceConfigValue(
                id,
                configDTO.getDescription(),
                configDTO.getValue(),
                configDTO.getStatus(),
                configDTO.getUpdatedBy()
        );

        //Update cache
        redisService.setValue("CONFIG:" + config.getName(), config.getValue(), 3600);

        return ResponseEntity.ok(config);
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
        return ResponseEntity.ok("Cache invalidate for: " + name);
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

}
