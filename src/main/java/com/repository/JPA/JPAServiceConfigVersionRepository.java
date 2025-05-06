package com.repository.JPA;

import com.model.ServiceConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;


import java.util.List;
import java.util.Optional;

@Repository
public interface JPAServiceConfigVersionRepository extends JpaRepository<ServiceConfigVersion, Long> {

    // Add this method to fetch the maximum version for a given config ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM ServiceConfigVersion v WHERE v.configId = :configId")
    int findMaxVersionForUpdate(@Param("configId") Long configId);


    List<ServiceConfigVersion> findByConfigIdOrderByVersionDesc(Long configId);

    Optional<ServiceConfigVersion> findByConfigIdAndVersion(Long configId, Integer version);

    void deleteByConfigId(Long configId);
}
