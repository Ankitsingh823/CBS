package com.controller;

import com.dto.ServiceConfigDTO;
import com.dto.ServiceConfigCompareDTO;
import com.model.ServiceConfiguration;
import com.service.ServiceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController

@RequestMapping("/api/config")
@CrossOrigin(origins = "*")

public class ServiceConfigController {

    @Autowired
    private ServiceConfigService serviceConfigService;

    @PostMapping("/create")
    public ResponseEntity<ServiceConfiguration> createServiceConfig(@RequestBody ServiceConfigDTO configDTO) {
        ServiceConfiguration config = serviceConfigService.createServiceConfig(
                configDTO.getName(),
                configDTO.getDescription(),
                configDTO.getValue()
        );
        return new ResponseEntity<>(config, HttpStatus.CREATED);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ServiceConfiguration> getServiceConfigById(@PathVariable long id)  {
        Optional<ServiceConfiguration> config = serviceConfigService.getServiceConfigById(id);
        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Object> getServiceConfigByName(@PathVariable String name) {
        return ResponseEntity.ok(serviceConfigService.getServiceConfigValue(name));
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
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceConfig(@PathVariable Long id) {
        serviceConfigService.deleteServiceConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invalidate-cache/{name}")
    public ResponseEntity<String> invalidateServiceConfigCache(@PathVariable String name) {
        serviceConfigService.invalidateServiceConfigCache(name);
        return ResponseEntity.ok("Cache invalidate for: " + name);
    }

    @GetMapping("/compare")
    public ResponseEntity<ServiceConfigCompareDTO> compareServiceConfigVersions(
            @RequestParam Long configId,
            @RequestParam(required = false) Integer version1,
            @RequestParam(required = false) Integer version2) {
        ServiceConfigCompareDTO comparison = serviceConfigService.compareServiceConfigVersions(configId, version1, version2);
        return ResponseEntity.ok(comparison);
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
