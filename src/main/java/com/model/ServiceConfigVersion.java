package com.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@RedisHash("ServiceConfigVersion")
@Entity
@Table(name = "service_config_version",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"config_id", "version"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ServiceConfigVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "config_id", nullable = false)
    @ToString.Include
    private Long configId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
}
