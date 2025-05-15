package com.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.data.redis.core.RedisHash;

@RedisHash("ServiceConfig")
@Entity
@Table(name = "service_configuration",
        indexes = {
                @Index(name = "idx_service_config_name", columnList = "name"),
                @Index(name = "idx_service_config_status", columnList = "status"),
                @Index(name = "idx_service_config_updated_at", columnList = "updated_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ServiceConfiguration {

    @Id
    @Column(nullable = false, unique = true)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Static method to generate random 8-digit ID
    public static Long generate8DigitId() {
        long min = 10000000L;
        long max = 99999999L;
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

}
