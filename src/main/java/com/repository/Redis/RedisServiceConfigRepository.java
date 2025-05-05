package com.repository.Redis;

import com.model.ServiceConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedisServiceConfigRepository extends CrudRepository<ServiceConfiguration, Long> {
    Optional<ServiceConfiguration> findByName(String name);

   /* @Query("insert")
    void saveToDb(ServiceConfiguration serviceConfiguration);*/
    Page<ServiceConfiguration> findByNameContaining(String name, Pageable pageable);

    Page<ServiceConfiguration> findByStatus(String status, Pageable pageable);

    Page<ServiceConfiguration> findByNameContainingAndStatus(String name, String status, Pageable pageable);

    List<ServiceConfiguration> findByStatus(String status);
}
