package com.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import lombok.Data;
import lombok.NoArgsConstructor;


import org.springframework.data.redis.core.RedisHash;

@RedisHash("ServiceConfig")
@Entity
@Table(name = "service_configuration",
        indexes = {
                @Index(name = "idx_service_config_name", columnList = "name"),
                @Index(name = "idx_service_config_status", columnList = "status"),
                @Index(name = "idx_service_config_updated_at", columnList = "updated_at")
        })
@Data
@NoArgsConstructor
public class ServiceConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt ;

    @Column
    private String updatedBy;

    @Column(nullable = false)
    private Integer version;


}
