package com.queueless.repository;

import com.queueless.entity.Provider;
import com.queueless.entity.enums.ProviderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUserId(Long userId);

    Page<Provider> findByStatus(ProviderStatus status, Pageable pageable);

    Page<Provider> findBySpecializationContainingIgnoreCase(String specialization, Pageable pageable);

    Page<Provider> findByStatusAndSpecializationContainingIgnoreCase(ProviderStatus status, String specialization, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Provider p WHERE p.id = :id")
    Optional<Provider> findWithLockById(@Param("id") Long id);
}
