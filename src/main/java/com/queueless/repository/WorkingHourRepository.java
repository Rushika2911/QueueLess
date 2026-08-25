package com.queueless.repository;

import com.queueless.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {

    List<WorkingHour> findByProviderId(Long providerId);

    List<WorkingHour> findByProviderIdAndDayOfWeek(Long providerId, DayOfWeek dayOfWeek);

    boolean existsByProviderIdAndDayOfWeek(Long providerId, DayOfWeek dayOfWeek);

    void deleteByProviderId(Long providerId);
}
