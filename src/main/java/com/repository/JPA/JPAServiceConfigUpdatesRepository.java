package com.repository.JPA;

import com.model.ServiceConfigUpdates;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JPAServiceConfigUpdatesRepository extends JpaRepository<ServiceConfigUpdates, Long> {

    Optional<ServiceConfigUpdates> findByConfigId(Long configId);

    List<ServiceConfigUpdates> findByConfigIdIn(List<Long> configIds);

    void deleteByConfigId(Long configId);
}