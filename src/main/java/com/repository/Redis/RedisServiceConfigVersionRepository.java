package com.repository.Redis;

import com.model.ServiceConfigVersion;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedisServiceConfigVersionRepository extends CrudRepository<ServiceConfigVersion, Long> {

    List<ServiceConfigVersion> findByConfigIdOrderByVersionDesc(Long configId);

    Optional<ServiceConfigVersion> findByConfigIdAndVersion(Long configId, Integer version);

    void deleteByConfigId(Long configId);
}
