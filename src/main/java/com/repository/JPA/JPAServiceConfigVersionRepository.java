package com.repository.JPA;

import com.model.ServiceConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JPAServiceConfigVersionRepository extends JpaRepository<ServiceConfigVersion, Long> {

    List<ServiceConfigVersion> findByConfigIdOrderByVersionDesc(Long configId);

    Optional<ServiceConfigVersion> findByConfigIdAndVersion(Long configId, Integer version);

    void deleteByConfigId(Long configId);
}
